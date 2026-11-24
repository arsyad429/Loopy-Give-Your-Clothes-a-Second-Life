# data

Implementasi data Android untuk Retrofit ke Go, preferensi DataStore, dan draft Room.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`local/`](local/README.md) | LoopyDatabase menyimpan draft pickup dengan Room. Preferensi tema memakai DataStore. |
| [`mapper/`](mapper/README.md) | Template mapper antara DTO, domain, dan cache. Konversi hasil pemeriksaan yang aktif ada di ClothingCheckDto. |
| [`remote/`](remote/README.md) | Interface Retrofit dan DTO untuk health, register, login, refresh, dan pemeriksaan pakaian. |
| [`repository/`](repository/README.md) | Repository untuk autentikasi, pemeriksaan pakaian, dan demo fondasi aplikasi, beserta template akses data fitur berikutnya. |
