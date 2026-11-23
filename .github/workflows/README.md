# Workflow CI

[ci.yml](ci.yml) berjalan saat push dan pull request.

| Job | Pemeriksaan |
| --- | --- |
| `android` | Struktur repository, assemble APK debug, unit test, lint, dan publikasi APK sebagai artefak CI. |
| `compose` | Validasi konfigurasi Docker Compose lokal dan LXC. |
| `go-backend` | Unit test Go, tes race dan integrasi PostgreSQL, vet, build, dan pemeriksaan dependensi yang rentan. |
