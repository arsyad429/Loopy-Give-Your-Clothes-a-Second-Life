# Jalur foto FileProvider

[file_paths.xml](file_paths.xml) mendeklarasikan `cache/clothing_photos/` sebagai lokasi berbagi foto sementara. [Manifest](../../AndroidManifest.xml) memasang provider dengan authority `${applicationId}.files`. Kamera memperoleh akses melalui URI yang diberikan oleh `ActivityResultContracts.TakePicture`.

[Implementasi penggunaan kamera](../../java/id/ac/loopy/feature/clothingcheck/ClothingDetectorScreen.kt)
