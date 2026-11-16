# model

Model domain sesuai tujuh entitas PRD §8. DonationStatus aktif, data class lain TODO.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `ClothingCheck.kt` | TEMPLATE PRD §8 — TODO: data class ClothingCheck: checkId, userId, clothingPhoto, clothingType, aiResult enum tiga kategori, aiDescription, createdAt. |
| `DonationRequest.kt` | TEMPLATE PRD §8 — TODO: data class DonationRequest: donationId, userId, checkId, phoneNumber, pickupAddress, city, quantity, preferredDate/Time, scheduledDate/Time nullable, notes, status, rejectionReason, createdAt/updatedAt. |
| `DonationStatus.kt` | Enum dan transisi status PRD untuk UI; backend tetap memvalidasi secara otoritatif. |
| `DonationStatusHistory.kt` | TEMPLATE PRD §8 — TODO: data class DonationStatusHistory: historyId, donationId, previousStatus nullable, newStatus, updatedBy, updatedAt. |
| `PointTransaction.kt` | TEMPLATE PRD §8 — TODO: data class PointTransaction: transactionId, userId, donationId atau redemptionId, type EARN/REDEEM, points signed, createdAt. Unique earn per donation di server. |
| `Redemption.kt` | TEMPLATE PRD §8 — TODO: data class Redemption: redemptionId, userId, rewardId, pointsUsed, rewardDetails, redeemedAt. |
| `Reward.kt` | TEMPLATE PRD §8 — TODO: data class Reward: rewardId, rewardName, description, image, requiredPoints, rewardType, isActive. Katalog tetap 10/20/30 poin. |
| `User.kt` | TEMPLATE PRD §8 — TODO: data class User: userId, name, email, role USER/ADMIN, totalPoints (server). Jangan simpan password dalam model/cache. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
