# dao

Room DAO untuk Flow observasi dan suspend write; cache bisnis wajib terfilter owner.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`ClothingCheckCacheDao.kt`](ClothingCheckCacheDao.kt) | TEMPLATE opsional — TODO: DAO ClothingCheck dengan query terfilter userId, Flow dan suspend writes. Tambahkan entity ke database dan migration serta uji isolasi antar akun. |
| [`DonationCacheDao.kt`](DonationCacheDao.kt) | TEMPLATE opsional — TODO: DAO Donation dengan query terfilter userId, Flow dan suspend writes. Tambahkan entity ke database dan migration serta uji isolasi antar akun. |
| [`PickupDraftDao.kt`](PickupDraftDao.kt) | Room Flow observasi draft, suspend upsert dan clear. |
| [`PointTransactionCacheDao.kt`](PointTransactionCacheDao.kt) | TEMPLATE opsional — TODO: DAO PointTransaction dengan query terfilter userId, Flow dan suspend writes. Tambahkan entity ke database dan migration serta uji isolasi antar akun. |
| [`RedemptionCacheDao.kt`](RedemptionCacheDao.kt) | TEMPLATE opsional — TODO: DAO Redemption dengan query terfilter userId, Flow dan suspend writes. Tambahkan entity ke database dan migration serta uji isolasi antar akun. |
| [`RewardCacheDao.kt`](RewardCacheDao.kt) | TEMPLATE opsional — TODO: DAO Reward dengan query terfilter userId, Flow dan suspend writes. Tambahkan entity ke database dan migration serta uji isolasi antar akun. |
| [`StatusHistoryCacheDao.kt`](StatusHistoryCacheDao.kt) | TEMPLATE opsional — TODO: DAO StatusHistory dengan query terfilter userId, Flow dan suspend writes. Tambahkan entity ke database dan migration serta uji isolasi antar akun. |
| [`UserCacheDao.kt`](UserCacheDao.kt) | TEMPLATE opsional — TODO: DAO User dengan query terfilter userId, Flow dan suspend writes. Tambahkan entity ke database dan migration serta uji isolasi antar akun. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
