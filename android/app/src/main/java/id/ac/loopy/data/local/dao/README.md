# Query Room

- [AdminPickupScheduleDraftDao.kt](AdminPickupScheduleDraftDao.kt) membaca, menyimpan, dan menghapus draf jadwal berdasarkan `adminId` serta `donationId`. Detail admin memakainya sebelum konfirmasi jadwal resmi.
- [PickupDraftDao.kt](PickupDraftDao.kt) menyediakan `Flow` observasi dan operasi tulis untuk draf fondasi. [Tes persistensi](../../../../../../../../androidTest/java/id/ac/loopy/FoundationPersistenceTest.kt)

File `*CacheDao.kt` menyimpan kerangka cache untuk fitur berikutnya. [Entity Room](../entity/README.md) · [Daftar DAO aktif](../LoopyDatabase.kt)
