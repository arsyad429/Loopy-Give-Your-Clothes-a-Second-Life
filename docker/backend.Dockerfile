# syntax=docker/dockerfile:1
FROM golang:1.27.4-alpine AS build
WORKDIR /src
COPY backend/go/go.mod backend/go/go.sum ./
RUN go mod download
COPY backend/go/ .
RUN go test ./... && go vet ./... && CGO_ENABLED=0 go build -trimpath -o /out/loopy .

FROM alpine:3.22
RUN apk add --no-cache ca-certificates && adduser -D -H -u 10001 loopy
WORKDIR /app
COPY --from=build --chown=loopy:loopy /out/loopy /app/loopy
USER loopy
ENV HTTP_ADDR=:8080
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=15s --retries=10 CMD ["/app/loopy", "healthcheck"]
ENTRYPOINT ["/app/loopy"]
