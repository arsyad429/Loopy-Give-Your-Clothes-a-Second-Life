# Komponen Compose bersama

| File | Kegunaan |
| --- | --- |
| [LoopyScreen.kt](LoopyScreen.kt) | Hierarki layar dengan judul, panah kembali, avatar, body scroll, tarik untuk menyegarkan, footer, dan navigasi bawah. |
| [CompletionComponents.kt](CompletionComponents.kt) | Kartu donasi, poin, ikon, waktu WIB, serta `UserBottomBar` dan `AdminBottomBar` yang aktif. |
| [LoopyButton.kt](LoopyButton.kt) dan [LoopyTextField.kt](LoopyTextField.kt) | Tombol dan input yang mengikuti ukuran, warna, serta aksesibilitas bersama. |
| [StatusBadge.kt](StatusBadge.kt) | Label visual untuk status pengajuan. |
| [ScheduleConfirmDialog.kt](ScheduleConfirmDialog.kt), [RejectRequestDialog.kt](RejectRequestDialog.kt), dan [PickupSuccessDialog.kt](PickupSuccessDialog.kt) | Dialog jadwal, penolakan, dan pengajuan berhasil. |
| [TemplateScreen.kt](TemplateScreen.kt) dan [TemplatePreview.kt](TemplatePreview.kt) | Scaffold dan pratinjau saat mengembangkan layar. |

Beberapa file satu fungsi bernama sama dengan komponen kartu atau bottom bar menyimpan kerangka lama. Cari implementasi aktif di `CompletionComponents.kt`. [Tema layar](../theme/README.md)
