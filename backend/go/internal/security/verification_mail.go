package security

import (
	"context"
	"crypto/tls"
	"errors"
	"fmt"
	"net"
	"net/smtp"
	"os"
	"strconv"
	"time"

	"loopy/backend/internal/domain"
)

type VerificationMailer struct {
	host, port, username, password, from, mode string
	tlsConfig                                  *tls.Config
}

// ConfiguredVerificationMailer reads SMTP settings for encrypted email delivery.
func ConfiguredVerificationMailer() (*VerificationMailer, error) {
	m := &VerificationMailer{
		host: os.Getenv("AUTH_SMTP_HOST"), port: os.Getenv("AUTH_SMTP_PORT"),
		username: os.Getenv("AUTH_SMTP_USER"), password: os.Getenv("AUTH_SMTP_PASSWORD"),
		from: os.Getenv("AUTH_SMTP_FROM"), mode: os.Getenv("AUTH_SMTP_TLS_MODE"),
	}
	if m.host == "" && m.from == "" && m.username == "" && m.password == "" {
		return nil, nil
	}
	if m.mode == "" {
		m.mode = "starttls"
	}
	if m.port == "" {
		m.port = "587"
		if m.mode == "implicit" {
			m.port = "465"
		}
	}
	port, err := strconv.Atoi(m.port)
	if err != nil || port < 1 || port > 65535 || m.host == "" || !domain.ValidEmail(m.from) ||
		(m.mode != "starttls" && m.mode != "implicit") || (m.username == "") != (m.password == "") {
		return nil, errors.New("invalid AUTH_SMTP_* configuration")
	}
	m.tlsConfig = &tls.Config{ServerName: m.host, MinVersion: tls.VersionTLS12}
	return m, nil
}

// SendVerificationCode submits a verification message through SMTP with mandatory TLS.
func (m *VerificationMailer) SendVerificationCode(ctx context.Context, email, code string) error {
	ctx, cancel := context.WithTimeout(ctx, 7*time.Second)
	defer cancel()

	dialer := &net.Dialer{}
	address := net.JoinHostPort(m.host, m.port)
	conn, err := dialer.DialContext(ctx, "tcp", address)
	if err != nil {
		return err
	}
	defer conn.Close()
	stop := context.AfterFunc(ctx, func() { _ = conn.Close() })
	defer stop()

	var smtpConn net.Conn = conn
	if m.mode == "implicit" {
		secure := tls.Client(conn, m.tlsConfig)
		if err = secure.HandshakeContext(ctx); err != nil {
			return err
		}
		smtpConn = secure
	}

	client, err := smtp.NewClient(smtpConn, m.host)
	if err != nil {
		return err
	}
	defer client.Close()
	if m.mode == "starttls" {
		if err = client.StartTLS(m.tlsConfig); err != nil {
			return err
		}
	}
	if m.username != "" {
		if err = client.Auth(smtp.PlainAuth("", m.username, m.password, m.host)); err != nil {
			return err
		}
	}
	if err = client.Mail(m.from); err != nil {
		return err
	}
	if err = client.Rcpt(email); err != nil {
		return err
	}
	writer, err := client.Data()
	if err != nil {
		return err
	}
	if _, err = fmt.Fprintf(writer, "From: %s\r\nTo: %s\r\nSubject: Kode verifikasi Loopy\r\n"+
		"MIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n"+
		"Kode verifikasi Loopy: %s\r\nKode berlaku selama 5 menit.\r\n", m.from, email, code); err != nil {
		return err
	}
	return writer.Close()
}
