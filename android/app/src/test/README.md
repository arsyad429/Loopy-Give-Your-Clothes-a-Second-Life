# Unit test Android di JVM

Source set ini menguji state ViewModel, aturan domain, serta request Retrofit dengan fake atau MockWebServer. Android SDK diperlukan untuk build modul. Unit test berjalan tanpa perangkat dan memakai fixture yang disiapkan suite.

```sh
./gradlew testDebugUnitTest
```

Jalankan dari folder `android/`. Laporan HTML berada di `app/build/reports/tests/testDebugUnitTest/`. [Peta paket tes](java/id/ac/loopy/README.md) · [Tes di perangkat](../androidTest/README.md)

## Subfolder

[java](java/README.md)
