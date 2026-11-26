# Domain Android

[Model](model/README.md) berisi bentuk data yang dipakai ViewModel dan Compose, termasuk status donasi dan hasil pemeriksaan. [Kontrak repository](repository/README.md) mendokumentasikan batas antara fitur dan layer data. Validasi akhir dan peran berasal dari backend.

Jika status atau payload berubah, periksa [domain Go](../../../../../../../../../backend/go/internal/domain/README.md), [mapper](../data/mapper/README.md), dan UI yang menampilkan status itu.
