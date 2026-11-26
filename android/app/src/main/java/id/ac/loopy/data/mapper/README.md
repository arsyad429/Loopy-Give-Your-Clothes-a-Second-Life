# Pemetaan DTO ke model aplikasi

[DonationMapper.kt](DonationMapper.kt) memetakan pengajuan, status, detail, dan request submit. [ClothingCheckMapper.kt](ClothingCheckMapper.kt) memetakan hasil pemeriksaan dan jenis pakaian. [RewardMapper.kt](RewardMapper.kt) memetakan katalog dan hasil penukaran.

`AuthMapper.kt` dan `AdminMapper.kt` berisi kerangka untuk pemetaan khusus. Alur admin saat ini memakai mapper donasi yang sama. [Tes mapper donasi](../../../../../../../test/java/id/ac/loopy/data/mapper/DonationMapperTest.kt) · [DTO](../remote/dto/README.md)
