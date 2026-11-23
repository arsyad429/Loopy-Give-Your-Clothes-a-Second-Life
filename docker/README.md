# Image dan konfigurasi backend

| File | Kegunaan |
| --- | --- |
| [backend.Dockerfile](backend.Dockerfile) | Build, tes, dan runtime non-root untuk API Go. |
| [vision.Dockerfile](vision.Dockerfile) | Build Gradle Java 17, tes, dan runtime non-root untuk API Ktor. Health check memanggil `/ready`. |
| [lxc.env.example](lxc.env.example) | Contoh konfigurasi deployment Go di LXC. |

Gunakan [compose.yaml](../compose.yaml) untuk Go lokal, [compose.lxc.yaml](../compose.lxc.yaml) untuk deployment Go, dan [compose.vision.yaml](../compose.vision.yaml) untuk Ktor. Compose Vision membuat proyek dan volume tersendiri. Jika sebelumnya kamu menjalankan Ktor dengan `compose.yaml`, pertahankan volume lama untuk backup atau migrasi. Untuk menjalankan Go lokal tanpa memakai volume lama itu, gunakan `docker compose -p loopy-go up --build -d --wait db backend`. Pilih port host yang tersedia untuk tiap API.

Image alat build Android didefinisikan di [Dockerfile root](../Dockerfile). [Panduan deployment LXC](../docs/deploy-lxc.md)
