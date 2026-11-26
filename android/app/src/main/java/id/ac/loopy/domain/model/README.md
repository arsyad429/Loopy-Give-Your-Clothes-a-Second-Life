# Model Kotlin untuk fitur

- [ClothingCheck.kt](ClothingCheck.kt) dan [ClothingType.kt](ClothingType.kt) mendefinisikan hasil AI serta jenis yang dikirim ke backend.
- [DonationRequest.kt](DonationRequest.kt), [DonationStatus.kt](DonationStatus.kt), dan [DonationStatusHistory.kt](DonationStatusHistory.kt) membawa data pengajuan, status, dan timeline.
- [Reward.kt](Reward.kt) dan [Redemption.kt](Redemption.kt) membawa katalog dan penukaran.

Status dan model wire dipetakan oleh [mapper](../../data/mapper/README.md). [Tes transisi status](../../../../../../../test/java/id/ac/loopy/domain/DonationStatusTest.kt)
