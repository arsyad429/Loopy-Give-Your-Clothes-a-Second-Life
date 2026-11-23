# scripts

Script untuk membangun Android di container serta memeriksa struktur Android dan backend Go.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`android-container.sh`](android-container.sh) | Snapshot source tanpa local.properties/build/cache host; ekspor APK, laporan, schema. |
| [`check-scaffold.py`](check-scaffold.py) | Read-only validasi 17 screen, README setiap folder, wrapper, 10 materi dan 20 AC. |
