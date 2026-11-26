# Image backend dan konfigurasi VM

[backend.Dockerfile](backend.Dockerfile) menguji dan membangun Go pada tahap build, lalu menjalankan binary non-root di Alpine. Healthcheck binary memakai `/ready`. [lxc.env.example](lxc.env.example) mencantumkan port, PostgreSQL, Groq, dan SMTP yang dipakai [Compose LXC](../compose.lxc.yaml).

Untuk VM yang sudah berjalan, edit `.env` di root repository dan gunakan kembali nama proyek serta volume yang sama. Nilai password PostgreSQL harus sesuai dengan role yang sudah dibuat. [Startup, HTTPS, dan backup](../docs/deploy-lxc.md) · [Compose development](../compose.yaml)
