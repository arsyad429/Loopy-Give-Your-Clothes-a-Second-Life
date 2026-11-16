# docker

Dockerfile backend multi-stage JDK17/JRE17 dengan runtime non-root. Android image didefinisikan pada Dockerfile root.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `backend.Dockerfile` | Build/test/installDist Kotlin, runtime JRE17 non-root dan healthcheck. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
