# workflows

CI build APK debug, unit/lint Android, artifact APK, Go auth unit/integration/race test dengan PostgreSQL, tes scaffold Ktor dan pemeriksaan Compose.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `ci.yml` | CI Android assemble/unit/lint dan upload APK; Go auth test/race/vet/build dengan PostgreSQL; Ktor tests/installDist dan Compose config. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
