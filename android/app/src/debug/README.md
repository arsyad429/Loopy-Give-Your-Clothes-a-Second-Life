# debug

Manifest debug mengizinkan HTTP untuk backend development yang diakses melalui ADB reverse.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`AndroidManifest.xml`](AndroidManifest.xml) | Activity/application/permission. Main menolak cleartext, overlay debug mengizinkan HTTP lokal. |
