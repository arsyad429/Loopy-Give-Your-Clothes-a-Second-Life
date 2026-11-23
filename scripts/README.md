# scripts

Script development/build container dan pemeriksaan kelengkapan. Jalankan structural checker dari root repo.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`android-container.sh`](android-container.sh) | Snapshot source tanpa local.properties/build/cache host; ekspor APK, laporan, schema. |
| [`check-scaffold.py`](check-scaffold.py) | Read-only validasi 17 screen, README setiap folder, wrapper, 10 materi dan 20 AC. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
