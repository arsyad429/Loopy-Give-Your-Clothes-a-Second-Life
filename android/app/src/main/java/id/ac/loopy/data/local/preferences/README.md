# Preferensi perangkat dengan DataStore

[UserPreferencesDataSource.kt](UserPreferencesDataSource.kt) membaca dan menulis `dark_theme` di `loopy_preferences`. Ia mengekspos `Flow` dengan default tema terang dan memakai preferensi kosong jika terjadi error I/O saat membaca. [MainActivity](../../../MainActivity.kt) mengamati nilai tema dari DataStore yang disediakan AppContainer.
