# Referensi produk, desain, dan PAPB

[PRD-v2.1.md](PRD-v2.1.md) mendefinisikan peran, status, cakupan MVP, dan kriteria penerimaan. Hubungkan rute serta node desain melalui [peta layar](../screen-map.md).

- [figma-structure.xml](figma-structure.xml) dan [figma-overview.png](figma-overview.png) menyimpan ekspor struktur dan gambaran desain.
- [s05-local-design.json](s05-local-design.json), [s05-android.png](s05-android.png), dan [local-design-thumbnail.png](local-design-thumbnail.png) memberi rujukan lokal untuk membandingkan layar pemeriksaan foto.
- [papb-catalog.json](papb-catalog.json) mencatat judul materi, metadata, dan checksum. [Pemetaan PAPB](../papb-mapping.md) menghubungkan materi dengan implementasi.

File desain sumber tersedia di [folder UI design](../../UI%20design/README.md).

## Cara memakai rujukan

- Saat mereview kebutuhan, mulai dari PRD dan catat ID fitur serta kriteria penerimaan. Gunakan [lampiran implementasi](PRD-v2.1.md#lampiran-rujukan-implementasi) untuk menghubungkan requirement dengan kode dan evidence.
- Saat mereview UI, cari ID layar pada [peta layar](../screen-map.md). Gunakan node Figma serta screenshot perangkat untuk state yang sama. Ekspor JSON, XML, dan PNG membantu memeriksa struktur secara lokal.
- Saat mereview tugas PAPB, cocokkan judul dan checksum di katalog, lalu buka [pemetaan materi](../papb-mapping.md) untuk contoh di repo. Katalog mendokumentasikan materi, sementara pengajaran tetap merujuk ke sumber kampus.
