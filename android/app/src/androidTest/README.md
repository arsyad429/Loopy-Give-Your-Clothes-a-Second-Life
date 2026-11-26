# Tes Android di perangkat

Source set ini memakai `AndroidJUnitRunner` untuk tes Compose, navigasi, dan migrasi Room. [Peta kelas tes](java/id/ac/loopy/README.md)

Jalankan suite di emulator atau perangkat yang dikhususkan untuk tes. Gradle dapat memasang dan menghapus paket Loopy sebagai bagian dari `connectedDebugAndroidTest`.

```sh
./gradlew connectedDebugAndroidTest
```

Jika perlu menguji pada HP yang menyimpan data Loopy, dari folder `android/` bangun APK aplikasi dan APK tes. Pasang keduanya dengan `adb install -r`. Gunakan URL backend yang sesuai dengan perangkat saat build.

```sh
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w \
  -e class id.ac.loopy.feature.ProfileNavigationTest \
  id.ac.loopy.test/androidx.test.runner.AndroidJUnitRunner
```

Contoh tersebut menjalankan satu kelas. Ubah atau hapus argumen `-e class` sesuai tes yang dibutuhkan. Proses aplikasi dapat dimulai ulang saat APK diperbarui atau saat instrumentasi berjalan, sehingga sesi di memori perlu dipulihkan dengan login.

## Subfolder

[java](java/README.md)
