# Riwayat schema LoopyDatabase

[1.json](1.json) memuat struktur awal `pickup_drafts`. [2.json](2.json) menambahkan tabel draf jadwal admin. Deklarasi auto-migration versi 1 ke 2 ada di [LoopyDatabase.kt](../../src/main/java/id/ac/loopy/data/local/LoopyDatabase.kt).

Untuk perubahan Room, cocokkan entity, versi database, snapshot, dan skenario migrasi perangkat sebelum merge.
