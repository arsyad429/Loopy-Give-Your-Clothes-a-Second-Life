# Paket internal backend

Folder ini berisi implementasi privat dari module Go Loopy. Alur request dimulai dari [httpapi](httpapi/README.md), memakai model dan kontrak [domain](domain/README.md), dan menyimpan hasil ke [store](store/README.md). Dependensi eksternal berada di [security](security/README.md) untuk pengiriman OTP dan [vision](vision/README.md) untuk Groq.

Perubahan endpoint biasanya menyentuh handler, kontrak domain, implementasi store, dan tes integrasi yang sesuai.
