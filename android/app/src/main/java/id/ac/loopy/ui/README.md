# Susunan UI dan rute

[LoopyApp.kt](LoopyApp.kt) mendefinisikan `NavHost`, mengamati sesi, dan merangkai ViewModel. S01 landing menjadi tujuan awal. Login memilih S04 untuk donatur atau A01 untuk admin. Rute terlindungi mengarahkan akun dengan peran yang sesuai. S09 dan A03 membawa ID pengajuan melalui rute.

`navigateToUserTab` menyimpan state tab donatur. Beranda memakai entri yang sudah ada saat kembali dari Donasiku atau Rewards. [TemplateViewModel.kt](TemplateViewModel.kt) membawa state tema DataStore untuk `MainActivity` serta aksi fondasi tes. [Navigasi](navigation/README.md) · [Komponen](components/README.md) · [Tema](theme/README.md)
