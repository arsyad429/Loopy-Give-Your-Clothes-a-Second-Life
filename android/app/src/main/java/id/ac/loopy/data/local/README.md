# Penyimpanan perangkat

[LoopyDatabase.kt](LoopyDatabase.kt) mendeklarasikan Room versi 2 dengan dua entity draf dan auto-migration 1 ke 2. Draf jadwal admin dipakai di detail donasi dan dikunci menurut admin serta donasi. Draf pickup contoh masih dipakai pada fondasi tes. [Schema Room](../../../../../../../../schemas/README.md)

[UserPreferencesDataSource](preferences/README.md) menyimpan tema melalui DataStore. Backend menjadi sumber resmi akun, donasi, saldo, dan rewards.

## Subfolder

[dao](dao/README.md) · [entity](entity/README.md)
