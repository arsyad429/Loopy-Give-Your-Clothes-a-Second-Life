# ui

Direktori ui mengelompokkan source/config Loopy. README subfolder menjelaskan implementasi spesifik.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`LoopyApp.kt`](LoopyApp.kt) | NavHost katalog debug dan alur landing, login, register, pratinjau admin, serta rute S05–S07 dengan checkId. |
| [`TemplateViewModel.kt`](TemplateViewModel.kt) | StateFlow combine/stateIn, coroutine scope, loading/error/cancellation untuk demo fondasi. |
| [`components/`](components/README.md) | Baca README di subfolder components untuk peran file dan TODO. |
| [`navigation/`](navigation/README.md) | Baca README di subfolder navigation untuk peran file dan TODO. |
| [`theme/`](theme/README.md) | Baca README di subfolder theme untuk peran file dan TODO. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
