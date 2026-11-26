# Skrip pengembangan

- [android-container.sh](android-container.sh) dipakai profil `tools` Compose. Skrip menyalin source Android ke workspace sementara, menjalankan Gradle, lalu mengembalikan APK dan laporan ke `artifacts/` serta schema Room ke `android/app/schemas/`.
- [check-scaffold.py](check-scaffold.py) memeriksa keberadaan layar, README folder, Gradle wrapper, backend Go, serta referensi dan kriteria penerimaan. Jalankan dari checkout bersih untuk mencocokkan pemeriksaan CI.

```sh
python3 scripts/check-scaffold.py
docker compose --profile tools run --rm android-dev assembleDebug testDebugUnitTest lintDebug
```

[Workflow CI](../.github/workflows/README.md) · [Panduan Android](../android/README.md)
