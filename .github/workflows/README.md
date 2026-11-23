# workflows

CI memeriksa struktur, build APK dan lint Android, tes Ktor, konfigurasi tiga Compose, serta tes Go dengan PostgreSQL.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`ci.yml`](ci.yml) | CI Android assemble/unit/lint dan upload APK; Go auth test/race/vet/build dengan PostgreSQL; Ktor tests/installDist dan Compose config. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
