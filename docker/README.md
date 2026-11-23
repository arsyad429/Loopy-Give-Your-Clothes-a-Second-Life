# docker

Dockerfile backend membangun Go dari `backend/go/`, menjalankan unit test dan vet, lalu menyalin binary ke runtime Alpine non-root. Readiness healthcheck menguji koneksi PostgreSQL melalui `/ready`. Android image JDK17 didefinisikan pada Dockerfile root. Baca [panduan auth](../docs/auth-go.md) untuk alur dan konfigurasi.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `backend.Dockerfile` | Build/test/vet Go, runtime non-root dan readiness healthcheck. |
| `lxc.env.example` | Contoh environment server LXC; password wajib diisi dan API bind address dapat dikonfigurasi. |

Deployment server menggunakan `docker compose -f compose.lxc.yaml up --build -d --wait` dari root repository. Ikuti [panduan LXC](../docs/deploy-lxc.md) untuk instalasi Docker, jaringan dan konfigurasi `.env`.

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
