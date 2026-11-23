# dto

Retrofit suspend API dan DTO. Health aktif; endpoint bisnis mengikuti docs/api-contract.md.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`AdminDto.kt`](AdminDto.kt) | TEMPLATE — TODO: request/response DTO Admin sesuai docs/api-contract.md. Nama field JSON, nullable, timestamps ISO-8601 dan error response harus disepakati backend. |
| [`AuthDto.kt`](AuthDto.kt) | TEMPLATE — TODO: request/response DTO Auth sesuai docs/api-contract.md. Nama field JSON, nullable, timestamps ISO-8601 dan error response harus disepakati backend. |
| [`ClothingCheckDto.kt`](ClothingCheckDto.kt) | TEMPLATE — TODO: request/response DTO ClothingCheck sesuai docs/api-contract.md. Nama field JSON, nullable, timestamps ISO-8601 dan error response harus disepakati backend. |
| [`DonationDto.kt`](DonationDto.kt) | TEMPLATE — TODO: request/response DTO Donation sesuai docs/api-contract.md. Nama field JSON, nullable, timestamps ISO-8601 dan error response harus disepakati backend. |
| [`HealthDto.kt`](HealthDto.kt) | DTO JSON status dan mode liveness backend. |
| [`RewardDto.kt`](RewardDto.kt) | TEMPLATE — TODO: request/response DTO Reward sesuai docs/api-contract.md. Nama field JSON, nullable, timestamps ISO-8601 dan error response harus disepakati backend. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
