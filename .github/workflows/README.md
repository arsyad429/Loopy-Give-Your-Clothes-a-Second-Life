# GitHub Actions

[ci.yml](ci.yml) menjalankan `Verify Loopy` untuk push dan pull request.

| Job | Verifikasi |
| --- | --- |
| `android` | Memasang SDK 36, mengecek struktur folder, membangun APK debug, menjalankan unit test dan lint, lalu mengunggah APK. |
| `compose` | Memvalidasi file Compose development dan LXC. |
| `go-backend` | Menjalankan tes dengan PostgreSQL 17 dan race detector, `go vet`, `go build`, serta `govulncheck`. |

Tes Compose UI dan Room di HP atau emulator dijalankan lewat [panduan instrumentasi Android](../../android/app/src/androidTest/README.md). Catat hasil uji perangkat dan provider di PR jika alurnya terdampak.
