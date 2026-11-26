# Akses data Android

ViewModel memanggil [repository](repository/README.md). Repository mengirim request melalui [Retrofit](remote/README.md), lalu [mapper](mapper/README.md) mengubah DTO ke model yang dipakai UI. [Data lokal](local/README.md) menangani draf serta preferensi perangkat.

Untuk menambah endpoint, mulai dari kontrak Go, tambahkan interface API dan DTO, lalu ubah repository serta tes yang memeriksa request/response. [Kontrak API](../../../../../../../../../docs/api-contract.md) · [Perakitan dependensi](../di/README.md)
