# Model domain Android

Model pemeriksaan dan status donasi dipakai oleh layar saat ini. Model bisnis lain masih berupa template PRD.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`ClothingCheck.kt`](ClothingCheck.kt) | Model hasil pemeriksaan pakaian, tiga kategori AiResult, deskripsi, rekomendasi dan waktu hasil. |
| [`DonationRequest.kt`](DonationRequest.kt) | TEMPLATE PRD §8 — TODO: data class DonationRequest: donationId, userId, checkId, phoneNumber, pickupAddress, city, quantity, preferredDate/Time, scheduledDate/Time nullable, notes, status, rejectionReason, createdAt/updatedAt. |
| [`DonationStatus.kt`](DonationStatus.kt) | Enum dan transisi status PRD untuk UI; backend tetap memvalidasi secara otoritatif. |
| [`DonationStatusHistory.kt`](DonationStatusHistory.kt) | TEMPLATE PRD §8 — TODO: data class DonationStatusHistory: historyId, donationId, previousStatus nullable, newStatus, updatedBy, updatedAt. |
| [`PointTransaction.kt`](PointTransaction.kt) | TEMPLATE PRD §8 — TODO: data class PointTransaction: transactionId, userId, donationId atau redemptionId, type EARN/REDEEM, points signed, createdAt. Unique earn per donation di server. |
| [`Redemption.kt`](Redemption.kt) | TEMPLATE PRD §8 — TODO: data class Redemption: redemptionId, userId, rewardId, pointsUsed, rewardDetails, redeemedAt. |
| [`Reward.kt`](Reward.kt) | TEMPLATE PRD §8 — TODO: data class Reward: rewardId, rewardName, description, image, requiredPoints, rewardType, isActive. Katalog tetap 10/20/30 poin. |
| [`User.kt`](User.kt) | TEMPLATE PRD §8 — TODO: data class User: userId, name, email, role USER/ADMIN, totalPoints (server). Jangan simpan password dalam model/cache. |
| [`ClothingType.kt`](ClothingType.kt) | Daftar 20 jenis pakaian, label dropdown, dan kode yang dikirim ke backend. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
