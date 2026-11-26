# Pemeriksaan foto dan Groq

[photo.go](photo.go) memeriksa MIME dan dimensi JPEG/PNG, membatasi upload hingga 10 MiB, menerapkan orientasi EXIF, lalu menormalisasi foto ke JPEG maksimum 2 MiB dan sisi terpanjang 1.600 piksel.

[groq.go](groq.go) mengirim foto terstandardisasi dan jenis pakaian ke Groq. Respons dibaca sebagai kategori, deskripsi, dan saran. Model dan format respons dibaca melalui `GROQ_MODEL` dan `GROQ_RESPONSE_MODE`. Backend memerlukan `GROQ_API_KEY` untuk mengaktifkan pemeriksaan.

[photo_test.go](photo_test.go) menguji penanganan format dan orientasi. [groq_test.go](groq_test.go) mencakup respons HTTP dan parsing hasil menggunakan respons uji. [Kontrak pemeriksaan](../../../../docs/api-contract.md#kontrak-pemeriksaan-pakaian-go)
