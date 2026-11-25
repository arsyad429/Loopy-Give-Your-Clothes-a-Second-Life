package security

import (
	"bufio"
	"context"
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

func TestVerificationMailerDeliversMessageOverTLS(t *testing.T) {
	certificate := httptest.NewTLSServer(http.NotFoundHandler())
	cert := certificate.TLS.Certificates[0]
	roots := x509.NewCertPool()
	roots.AddCert(certificate.Certificate())
	certificate.Close()

	listener, err := tls.Listen("tcp", "127.0.0.1:0", &tls.Config{Certificates: []tls.Certificate{cert}})
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()

	result := make(chan error, 1)
	go func() {
		conn, err := listener.Accept()
		if err != nil {
			result <- err
			return
		}
		defer conn.Close()
		_ = conn.SetDeadline(time.Now().Add(5 * time.Second))
		result <- receiveTestMessage(conn)
	}()

	host, port, err := net.SplitHostPort(listener.Addr().String())
	if err != nil {
		t.Fatal(err)
	}
	mailer := VerificationMailer{
		host: host, port: port, mode: "implicit", from: "sender@example.test",
		username: "test", password: "test-password",
		tlsConfig: &tls.Config{ServerName: host, RootCAs: roots, MinVersion: tls.VersionTLS12},
	}
	if err := mailer.SendVerificationCode(context.Background(), "user@example.test", "234567"); err != nil {
		t.Fatal(err)
	}
	if err := <-result; err != nil {
		t.Fatal(err)
	}
}

func receiveTestMessage(conn io.ReadWriter) error {
	read := bufio.NewReader(conn)
	steps := []struct {
		reply, prefix string
	}{
		{"220 loopy.example.test\r\n", "EHLO "},
		{"250-loopy.example.test\r\n250 AUTH PLAIN\r\n", "AUTH PLAIN "},
		{"235 Authentication accepted\r\n", "MAIL FROM:<sender@example.test>"},
		{"250 Sender accepted\r\n", "RCPT TO:<user@example.test>"},
		{"250 Recipient accepted\r\n", "DATA"},
	}
	for _, step := range steps {
		if _, err := io.WriteString(conn, step.reply); err != nil {
			return err
		}
		line, err := read.ReadString('\n')
		if err != nil {
			return err
		}
		if !strings.HasPrefix(line, step.prefix) {
			return fmt.Errorf("expected SMTP command %q", step.prefix)
		}
	}
	if _, err := io.WriteString(conn, "354 End with a dot\r\n"); err != nil {
		return err
	}
	var body strings.Builder
	for {
		line, err := read.ReadString('\n')
		if err != nil {
			return err
		}
		if line == ".\r\n" {
			break
		}
		body.WriteString(line)
	}
	if !strings.Contains(body.String(), "To: user@example.test\r\n") ||
		!strings.Contains(body.String(), "Kode verifikasi Loopy: 234567\r\n") {
		return fmt.Errorf("message missing recipient or verification code")
	}
	_, err := io.WriteString(conn, "250 Message accepted\r\n")
	return err
}
