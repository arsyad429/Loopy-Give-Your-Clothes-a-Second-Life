# app

Modul APK id.ac.loopy, minSdk24/targetSdk36, debug harness fondasi dan template fitur PRD. Bukan implementasi MVP final.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `build.gradle.kts` | Plugin, dependency dan task build modul/project; jaga kompatibilitas versi. |
| `proguard-rules.pro` | Tempat aturan R8 tambahan setelah shrinking diuji nanti. |
| `schemas/` | Baca README di subfolder schemas untuk peran file dan TODO. |
| `src/` | Baca README di subfolder src untuk peran file dan TODO. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
