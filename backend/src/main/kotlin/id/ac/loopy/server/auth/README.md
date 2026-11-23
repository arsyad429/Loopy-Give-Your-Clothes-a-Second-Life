# Sesi API pemeriksaan Ktor

Token opaque mengidentifikasi akun dan role dari PostgreSQL. Store menyimpan hash token dan menolak sesi yang kedaluwarsa atau dicabut.

| File atau folder | Kegunaan |
| --- | --- |
| [`Authorization.kt`](Authorization.kt) | Principal pengguna, role, dan pemasangan plugin bearer Ktor. |
| [`SessionStore.kt`](SessionStore.kt) | Pembuatan, verifikasi, dan pencabutan sesi pemeriksaan. |
| [`AuthRoutes.kt`](AuthRoutes.kt) | Template route akun Ktor. |
| [`AuthService.kt`](AuthService.kt) | Template validasi kredensial akun Ktor. |

Login, register, dan refresh yang tersedia saat ini ada di [API Go](../../../../../../../../go/README.md). Sesi Go memerlukan integrasi terpisah sebelum bisa dipakai untuk pemeriksaan Ktor.
