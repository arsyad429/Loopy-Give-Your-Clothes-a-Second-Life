# Penyimpanan foto privat

Foto normalisasi ditulis sebagai JPEG di PostgreSQL dalam transaksi hasil pemeriksaan yang sama.

| File atau folder | Kegunaan |
| --- | --- |
| [`PhotoStorage.kt`](PhotoStorage.kt) | Menyimpan byte foto dan membacanya melalui pemeriksaan akun pemilik atau admin. |
