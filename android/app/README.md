# app

Modul APK id.ac.loopy untuk API 24–36. Katalog debug membuka pratinjau layar, termasuk pemeriksaan pakaian S05 dan hasil S06.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`build.gradle.kts`](build.gradle.kts) | Plugin, dependency dan task build modul/project; jaga kompatibilitas versi. |
| [`proguard-rules.pro`](proguard-rules.pro) | Tempat aturan R8 tambahan setelah shrinking diuji nanti. |
| [`schemas/`](schemas/README.md) | Baca README di subfolder schemas untuk peran file dan TODO. |
| [`src/`](src/README.md) | Baca README di subfolder src untuk peran file dan TODO. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
