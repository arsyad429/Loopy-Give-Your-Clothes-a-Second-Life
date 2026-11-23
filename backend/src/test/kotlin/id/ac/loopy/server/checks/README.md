# Tes API pemeriksaan pakaian Ktor

| File | Cakupan |
| --- | --- |
| [AiVisionGatewayTest.kt](AiVisionGatewayTest.kt) | Payload Groq, tiga kategori hasil, respons invalid, rate limit, timeout, serta smoke test Groq opsional. |
| [ClothingCheckStoreIntegrationTest.kt](ClothingCheckStoreIntegrationTest.kt) | Sesi bearer, akses pemilik dan admin, rollback foto, serta idempotensi di PostgreSQL. |
| [ClothingCheckRoutesTest.kt](ClothingCheckRoutesTest.kt) | Validasi upload, create dan replay, serta privasi hasil dan foto melalui HTTP. Mode live mengirim foto uji ke Groq. |

Lihat [cara menjalankan tes](../../../../../../../../README.md) untuk konfigurasi database uji dan mode live.
