# Image dan konfigurasi backend

Image backend Go dan contoh konfigurasi deployment Docker.

| File | Kegunaan |
| --- | --- |
| [backend.Dockerfile](backend.Dockerfile) | Build dan tes Go, runtime non-root, serta pemeriksaan status layanan. |
| [lxc.env.example](lxc.env.example) | Contoh konfigurasi PostgreSQL, port API, dan Groq untuk deployment LXC. |

Compose lokal dan LXC membangun satu API Go untuk login, pemeriksaan, dan admin. Untuk instalasi development dengan volume terpisah, gunakan `API_PORT=8082 docker compose -p loopy-go up --build -d --wait db backend` agar Go memperoleh volume sendiri. Simpan volume backend lama sebagai arsip jika masih diperlukan. Untuk instalasi Go yang sudah ada, gunakan proyek dan volume Go yang sama agar versi skema terbaru diterapkan pada data tersebut. [Compose lokal](../compose.yaml) · [Compose LXC](../compose.lxc.yaml)

Image alat build Android didefinisikan di [Dockerfile root](../Dockerfile). [Panduan deployment LXC](../docs/deploy-lxc.md)
