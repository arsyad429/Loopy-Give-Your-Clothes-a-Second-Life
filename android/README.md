# Proyek Android

Buka folder ini di Android Studio atau jalankan Gradle Wrapper. Gunakan JDK 17, SDK Platform 36, dan Build Tools 36.0.0. Modul [app](app/README.md) menghasilkan APK untuk Android API 24 ke atas.

## Menjalankan aplikasi

```sh
./gradlew assembleDebug -PLOOPY_API_BASE_URL=http://10.0.2.2:8080/
```

URL itu menunjuk port 8080 di host emulator. Sesuaikan dengan port backend yang sedang dipakai. Pada HP dengan USB debugging, jalankan `adb reverse tcp:8080 tcp:8080` di laptop dan gunakan `http://127.0.0.1:8080/` untuk build. Untuk backend VM yang bind ke loopback, teruskan port dari VM ke laptop melalui SSH sebelum `adb reverse`. Properti URL debug harus berakhir dengan `/`. [Koneksi perangkat](../docs/development.md#koneksi-android-ke-backend) · [Koneksi ke VM](../docs/deploy-lxc.md#7-connect-android-or-a-public-domain)

## Build dan tes

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

APK ada di `app/build/outputs/apk/debug/app-debug.apk`. [Tes perangkat](app/src/androidTest/README.md) mencakup Compose dan Room. [build.gradle.kts](build.gradle.kts) mengatur plugin root, [settings.gradle.kts](settings.gradle.kts) mendaftarkan modul, dan [gradle.properties](gradle.properties) menyimpan pengaturan build bersama. Gunakan `gradlew.bat` di Windows.

Build release menggunakan `LOOPY_RELEASE_API_BASE_URL` dengan HTTPS dan perlu penandatanganan untuk distribusi. [Panduan release](../docs/deploy-lxc.md#7-connect-android-or-a-public-domain)

## Subfolder

[gradle](gradle/README.md)
