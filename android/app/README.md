# Modul aplikasi Loopy

Modul `id.ac.loopy` menggabungkan Compose, Navigation, Retrofit, Room, dan DataStore. Mulai dari [source main](src/main/README.md) untuk mengikuti alur aplikasi dan [AppContainer](src/main/java/id/ac/loopy/di/AppContainer.kt) untuk melihat susunan dependensinya.

[build.gradle.kts](build.gradle.kts) menetapkan SDK, URL backend per build type, Java 17, serta ekspor schema Room. Task `prepareMainResources` menyalin resource ke direktori build dan mengecualikan README serta Markdown lisensi dari input compiler Android. [proguard-rules.pro](proguard-rules.pro) menampung aturan R8 modul. Minifikasi release saat ini dinonaktifkan.

Gunakan `assembleDebug` untuk APK, `testDebugUnitTest` untuk JVM, dan `assembleDebugAndroidTest` untuk APK tes perangkat. [Perintah Gradle](../README.md)

## Subfolder

[schemas](schemas/README.md) · [src](src/README.md)
