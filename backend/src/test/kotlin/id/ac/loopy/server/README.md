# Tes server Kotlin/Ktor

Tes memisahkan liveness, pemeriksaan pakaian, dan rencana pengujian fitur lanjutan.

| File atau folder | Kegunaan |
| --- | --- |
| [`HealthTest.kt`](HealthTest.kt) | Memastikan route liveness /health merespons saat modul dasar dimuat. |
| [`checks`](checks/README.md) | Tes payload Groq, store PostgreSQL, route pemeriksaan, dan foto privat. |
| [`AuthorizationTest.kt`](AuthorizationTest.kt) | Template pengujian akses umum untuk fitur Ktor lanjutan. |
| [`PickupValidationTest.kt`](PickupValidationTest.kt) | Template pengujian validasi pengajuan pickup Ktor. |
| [`PointTransactionTest.kt`](PointTransactionTest.kt) | Template pengujian transaksi poin Ktor. |
| [`RedemptionTransactionTest.kt`](RedemptionTransactionTest.kt) | Template pengujian transaksi redemption Ktor. |
