package main

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"net"
	"net/http"
	"net/url"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"loopy/backend/internal/domain"
	"loopy/backend/internal/httpapi"
	"loopy/backend/internal/store"
	"loopy/backend/internal/vision"
)

func main() {
	if err := run(); err != nil {
		slog.Error(err.Error()) // run returns safe messages without credentials.
		os.Exit(1)
	}
}

func databaseURL() (string, error) {
	if value := os.Getenv("DATABASE_URL"); value != "" {
		return value, nil
	}
	if os.Getenv("POSTGRES_USER") == "" || os.Getenv("POSTGRES_PASSWORD") == "" || os.Getenv("POSTGRES_DB") == "" {
		return "", errors.New("set DATABASE_URL or POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB")
	}
	u := url.URL{
		Scheme: "postgres",
		Host:   net.JoinHostPort(envOr("DB_HOST", "db"), "5432"),
		User:   url.UserPassword(os.Getenv("POSTGRES_USER"), os.Getenv("POSTGRES_PASSWORD")),
		Path:   "/" + os.Getenv("POSTGRES_DB"),
	}
	u.RawQuery = url.Values{"sslmode": {envOr("DATABASE_SSLMODE", "require")}}.Encode()
	return u.String(), nil
}

func envOr(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}

func run() error {
	command := "serve"
	if len(os.Args) > 1 {
		command = os.Args[1]
	}
	if len(os.Args) > 2 || (command != "serve" && command != "create-admin" && command != "healthcheck") {
		return errors.New("usage: loopy [serve|create-admin|healthcheck]")
	}
	if command == "healthcheck" {
		return runHealthcheck()
	}

	connection, err := databaseURL()
	if err != nil {
		return err
	}
	cfg, err := pgxpool.ParseConfig(connection)
	if err != nil {
		return errors.New("invalid database configuration")
	}
	cfg.MaxConns = 10
	cfg.ConnConfig.ConnectTimeout = 5 * time.Second

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	pool, err := pgxpool.NewWithConfig(ctx, cfg)
	if err != nil {
		return errors.New("database pool could not be initialized")
	}
	defer pool.Close()

	pg := store.NewPGStore(pool)
	if err = pg.Ping(ctx); err != nil {
		return errors.New("database connection failed; check connection settings and availability")
	}
	if err = pg.Migrate(ctx); err != nil {
		return errors.New("database migration failed; verify database permissions and schema")
	}
	if command == "create-admin" {
		return createAdmin(ctx, pg)
	}

	auth, err := httpapi.NewAuthService(pg)
	if err != nil {
		return errors.New("password hashing initialization failed")
	}
	analyzer, err := vision.ConfiguredFromEnv()
	if err != nil {
		return err
	}
	checks := httpapi.NewClothingCheckService(pg, analyzer)
	handler := httpapi.NewServer(auth, pg, checks).Routes()

	server := &http.Server{
		Addr:              envOr("HTTP_ADDR", "127.0.0.1:8080"),
		Handler:           handler,
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       15 * time.Second,
		WriteTimeout:      45 * time.Second,
		IdleTimeout:       60 * time.Second,
		MaxHeaderBytes:    16 * 1024,
	}

	stop, stopSignals := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stopSignals()

	failed := make(chan error, 1)
	go func() { failed <- server.ListenAndServe() }()
	slog.Info("Loopy Go API started", "address", server.Addr)

	select {
	case err := <-failed:
		if !errors.Is(err, http.ErrServerClosed) {
			return errors.New("HTTP server failed to start or stopped unexpectedly")
		}
		return nil
	case <-stop.Done():
		shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer shutdownCancel()
		if err := server.Shutdown(shutdownCtx); err != nil {
			_ = server.Close()
			return errors.New("HTTP shutdown timed out")
		}
		return nil
	}
}

func runHealthcheck() error {
	client := http.Client{Timeout: 2 * time.Second}
	response, err := client.Get("http://127.0.0.1:8080/ready")
	if err != nil {
		return errors.New("readiness check failed")
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		return errors.New("readiness check failed")
	}
	return nil
}

func createAdmin(ctx context.Context, pg *store.PGStore) error {
	req, err := domain.ValidateRegistration(domain.RegisterRequest{
		Name:     os.Getenv("LOOPY_ADMIN_NAME"),
		Email:    os.Getenv("LOOPY_ADMIN_EMAIL"),
		Password: os.Getenv("LOOPY_ADMIN_PASSWORD"),
	})
	if err != nil {
		return fmt.Errorf("set valid LOOPY_ADMIN_NAME, LOOPY_ADMIN_EMAIL, LOOPY_ADMIN_PASSWORD (%d–%d characters)", domain.MinPasswordLength, domain.MaxPasswordLength)
	}
	account, err := httpapi.MakeAccount(req)
	if err != nil {
		return errors.New("admin password hashing failed")
	}
	user, err := pg.CreateAdmin(ctx, account)
	if errors.Is(err, domain.ErrDuplicateEmail) {
		return errors.New("admin not created: email already registered")
	}
	if err != nil {
		return errors.New("admin could not be created")
	}
	fmt.Printf("Admin created: %s\n", user.ID)
	return nil
}
