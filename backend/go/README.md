# Loopy Go backend

See the [backend guide](../../docs/backend.md) for the architecture and setup, the [auth guide](../../docs/auth-go.md) for account and session details, and the [donation review guide](../../docs/donation-review-go.md) for the admin API and a local fixture.

This is the active Docker backend: Go `net/http`, PostgreSQL via pgx, Argon2id passwords, and opaque access/refresh sessions. The sibling Gradle project is the earlier Ktor scaffold.

- `main.go`: configuration, DB startup/migration, HTTP server, private `create-admin` command.
- `schema.sql`, `schema_pickup.sql`, `store.go`: ordered database migrations and auth persistence.
- `password.go`, `auth.go`: password hashing and account/session rules.
- `http.go`: routes, middleware, bounded JSON input, errors, rate limits.
- `donation.go`, `donation_http.go`, `donation_store.go`: admin review, pickup scheduling, and audit history.
- `*_test.go`: unit tests and real PostgreSQL verification.

Use Go 1.27.4+ (or automatic toolchain selection), set `DATABASE_URL` to a PostgreSQL URL and run `go run .`. Default `HTTP_ADDR` is `127.0.0.1:8080`; Compose sets `:8080` inside the container. From the repo root, `docker compose up --build -d --wait` starts both services. Public routes: `/health`, `/ready`, register/login/refresh. `/v1/auth/me` and logout require a Bearer access token.

Run `go test ./...` and `go vet ./...`. Integration tests run when `TEST_DATABASE_URL` is set, and isolate their tables in a temporary schema. Never use a production database. On systems with a C compiler, `go test -race ./...` adds race detection. Check known reachable vulnerabilities with `go run golang.org/x/vuln/cmd/govulncheck@v1.9.0 ./...`; CI runs this check too.

No account is seeded automatically. To provision a new admin, privately set `LOOPY_ADMIN_NAME`, `LOOPY_ADMIN_EMAIL`, and `LOOPY_ADMIN_PASSWORD`, then run `go run . create-admin`. Existing emails are refused. See the guide for Docker usage, exact JSON, Android integration requirements, and limits.
