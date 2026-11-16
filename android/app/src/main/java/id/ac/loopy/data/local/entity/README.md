# entity

Room entity draft aktif dan template cache per user; backend sumber kebenaran bisnis.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `ClothingCheckCacheEntity.kt` | TEMPLATE opsional — TODO: cache ClothingCheck ber-key userId; minimalkan PII, TTL dan invalidasi logout. Room hanya cache/draft; otoritas status, role dan poin di backend. |
| `DonationCacheEntity.kt` | TEMPLATE opsional — TODO: cache Donation ber-key userId; minimalkan PII, TTL dan invalidasi logout. Room hanya cache/draft; otoritas status, role dan poin di backend. |
| `PickupDraftEntity.kt` | Entity contoh draft lokal tanpa PII, bukan pengajuan resmi backend. |
| `PointTransactionCacheEntity.kt` | TEMPLATE opsional — TODO: cache PointTransaction ber-key userId; minimalkan PII, TTL dan invalidasi logout. Room hanya cache/draft; otoritas status, role dan poin di backend. |
| `RedemptionCacheEntity.kt` | TEMPLATE opsional — TODO: cache Redemption ber-key userId; minimalkan PII, TTL dan invalidasi logout. Room hanya cache/draft; otoritas status, role dan poin di backend. |
| `RewardCacheEntity.kt` | TEMPLATE opsional — TODO: cache Reward ber-key userId; minimalkan PII, TTL dan invalidasi logout. Room hanya cache/draft; otoritas status, role dan poin di backend. |
| `StatusHistoryCacheEntity.kt` | TEMPLATE opsional — TODO: cache StatusHistory ber-key userId; minimalkan PII, TTL dan invalidasi logout. Room hanya cache/draft; otoritas status, role dan poin di backend. |
| `UserCacheEntity.kt` | TEMPLATE opsional — TODO: cache User ber-key userId; minimalkan PII, TTL dan invalidasi logout. Room hanya cache/draft; otoritas status, role dan poin di backend. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
