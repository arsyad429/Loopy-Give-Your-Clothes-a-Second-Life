# remote

Interface Retrofit dan DTO untuk health, register, login, refresh, dan pemeriksaan pakaian.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`api/`](api/README.md) | Deklarasi endpoint Retrofit untuk health, sesi, dan pemeriksaan yang disediakan oleh Go. |
| [`datasource/`](datasource/README.md) | Template pembungkus panggilan jarak jauh. Repository auth dan pemeriksaan aktif memakai Retrofit secara langsung. |
| [`dto/`](dto/README.md) | Bentuk JSON untuk Retrofit, termasuk health, akun, token sesi, dan hasil pemeriksaan. |
