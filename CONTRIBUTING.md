# Kontribusi Loopy

1. Baca README folder dan AC fitur. Sepakati pemilik tugas di tim.
2. Buat branch feat/<fitur>, fix/<masalah>, atau docs/<topik> dari main.
3. UI menerima state/callback; ViewModel memakai repository, tanpa DAO/Retrofit langsung. Teruskan CancellationException.
4. Dependency Android melalui version catalog. Jangan commit .env, local.properties, key/token/password, atau foto/alamat nyata.
5. Jalankan structural check, build/lint/unit test yang relevan, dan verifikasi UI di emulator terhadap Figma.
6. Perbarui README folder, backlog, kontrak dan tes saat mengisi TEMPLATE; placeholder bukan fitur selesai.
7. Buka PR dengan perilaku akhir, kaitan Fxx/ACxx, bukti cek dan batasan. Minta review rekan.

Selesai berarti UI, state, repository, server authorization/transaksi terkait, error/loading/empty dan AC sudah terbukti bekerja.
