# Katalog versi Android dan Wrapper

[libs.versions.toml](libs.versions.toml) memusatkan versi AGP, Kotlin, KSP, Compose, Navigation, Room, dan Retrofit serta dependency tes. Modul memanggil alias katalog dari [app/build.gradle.kts](../app/build.gradle.kts). Saat mengganti versi, cek kompatibilitas plugin dan jalankan build, unit test, serta lint.

[Wrapper](wrapper/README.md) menentukan versi Gradle yang dipakai laptop dan CI.
