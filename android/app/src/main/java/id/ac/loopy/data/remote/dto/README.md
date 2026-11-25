# dto

Bentuk JSON untuk Retrofit, termasuk health, akun, token sesi, dan hasil pemeriksaan.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`AdminDto.kt`](AdminDto.kt) | TEMPLATE — TODO: request/response DTO Admin sesuai docs/api-contract.md. Nama field JSON, nullable, timestamps ISO-8601 dan error response harus disepakati backend. |
| [`AuthDto.kt`](AuthDto.kt) | Payload login, register, dan refresh, serta DTO sesi dan akun. |
| [`ClothingCheckDto.kt`](ClothingCheckDto.kt) | Respons hasil dari Go dan konversi ke model domain ClothingCheck. |
| [`DonationDto.kt`](DonationDto.kt) | TEMPLATE — TODO: request/response DTO Donation sesuai docs/api-contract.md. Nama field JSON, nullable, timestamps ISO-8601 dan error response harus disepakati backend. |
| [`HealthDto.kt`](HealthDto.kt) | DTO JSON status dan mode liveness backend. |
| [`RewardDto.kt`](RewardDto.kt) | JSON katalog reward, saldo, redemption dan hasil replay dari API Go. |
