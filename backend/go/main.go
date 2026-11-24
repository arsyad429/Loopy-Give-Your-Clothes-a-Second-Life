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
	u := url.URL{Scheme: "postgres", Host: net.JoinHostPort(envOr("DB_HOST", "db"), "5432"),
		User: url.UserPassword(os.Getenv("POSTGRES_USER"), os.Getenv("POSTGRES_PASSWORD")), Path: "/" + os.Getenv("POSTGRES_DB")}
	query := url.Values{"sslmode": {envOr("DATABASE_SSLMODE", "require")}}
	u.RawQuery = query.Encode()
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
		client := http.Client{Timeout: 2 * time.Second}
		response, err := client.Get("http://127.0.0.1:8080/ready")
		if err != nil {
			return errors.New("readiness check failed")
		}
		defer response.Body.Close()
		if response.StatusCode != 200 {
			return errors.New("readiness check failed")
		}
		return nil
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
	store := &pgStore{pool: pool}
	if err = store.Ping(ctx); err != nil {
		return errors.New("database connection failed; check connection settings and availability")
	}
	if err = store.migrate(ctx); err != nil {
		return errors.New("database migration failed; verify database permissions and schema")
	}
	if command == "create-admin" {
		return createAdmin(ctx, store)
	}
	auth, err := newAuthService(store)
	if err != nil {
		return errors.New("password hashing initialization failed")
	}
	vision, err := configuredVision()
	if err != nil {
		return err
	}
	handler := newAPI(auth, store)
	handler.checks = &clothingCheckService{store: store, vision: vision}
	server := &http.Server{Addr: envOr("HTTP_ADDR", "127.0.0.1:8080"), Handler: handler.routes(),
		ReadHeaderTimeout: 5 * time.Second, ReadTimeout: 15 * time.Second, WriteTimeout: 45 * time.Second,
		IdleTimeout: 60 * time.Second, MaxHeaderBytes: 16 * 1024}
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

func createAdmin(ctx context.Context, store repository) error {
	req, err := validateRegistration(registerRequest{Name: os.Getenv("LOOPY_ADMIN_NAME"), Email: os.Getenv("LOOPY_ADMIN_EMAIL"), Password: os.Getenv("LOOPY_ADMIN_PASSWORD")})
	if err != nil {
		return errors.New("set valid LOOPY_ADMIN_NAME, LOOPY_ADMIN_EMAIL, LOOPY_ADMIN_PASSWORD (15–128 characters)")
	}
	a, err := makeAccount(req)
	if err != nil {
		return errors.New("admin password hashing failed")
	}
	u, err := store.CreateAdmin(ctx, a)
	if errors.Is(err, errDuplicateEmail) {
		return errors.New("admin not created: email already registered")
	}
	if err != nil {
		return errors.New("admin could not be created")
	}
	fmt.Printf("Admin created: %s\n", u.ID)
	return nil
}
