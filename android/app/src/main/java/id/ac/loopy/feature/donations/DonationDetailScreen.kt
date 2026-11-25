package id.ac.loopy.feature.donations

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.feature.pickup.PickupRequestValidator
import id.ac.loopy.ui.components.*
import java.util.Locale

/** S09 (Akun aktif): detail & timeline status donasi dari backend Go. */
@Composable
fun DonationDetailScreen(
    viewModel: DonationDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoopyScreen("Detail donasi", onBack) {
        when {
            state.isLoading -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Memuat detail donasi…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            state.error != null -> {
                LoopyInfo("Belum berhasil", state.error.orEmpty(), isError = true)
                LoopyButton("Muat ulang", viewModel::retry)
            }
            state.donation == null -> {
                LoopyInfo("Pengajuan tidak ditemukan", "Kembali ke Donasiku untuk memilih pengajuan.")
            }
            else -> {
                val item = state.donation!!
                val status = item.status
                val shortId = item.donationId.substringBefore('-').uppercase(Locale.US)
                Text("#$shortId · ${item.quantity} pakaian · ${item.clothingCheck.clothingType.label}", style = MaterialTheme.typography.bodySmall)
                Text(
                    if (status == DonationStatus.COMPLETED) "Kebaikanmu\nsudah tersampaikan." else "Kebaikanmu\nsedang diproses.",
                    style = MaterialTheme.typography.headlineMedium,
                )
                StatusBadge(status)
                Text(
                    when (status) {
                        DonationStatus.COMPLETED -> "Penerimaan pakaian sudah dikonfirmasi. 10 poin telah ditambahkan satu kali."
                        DonationStatus.PICKED_UP -> "Pakaian sudah dijemput. Menunggu konfirmasi penerimaan dari admin."
                        DonationStatus.PICKUP_SCHEDULED -> "Penjemputan sudah dijadwalkan oleh admin."
                        DonationStatus.REJECTED -> "Pengajuan ditolak oleh admin."
                        DonationStatus.PENDING_REVIEW -> "Pengajuan terkirim. Admin akan memeriksa data dan mengonfirmasi jadwal penjemputan."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (status == DonationStatus.REJECTED && !item.rejectionReason.isNullOrBlank()) {
                    LoopyInfo("Alasan penolakan", item.rejectionReason, isError = true)
                }
                if (item.scheduledDate != null && item.scheduledTime != null) {
                    LoopyInfo(
                        "Jadwal final penjemputan",
                        "${PickupRequestValidator.formatReadableDate(item.scheduledDate)} · ${item.scheduledTime} WIB",
                    )
                }
                LoopyInfo(
                    "Preferensi penjemputanmu",
                    "${PickupRequestValidator.formatReadableDate(item.preferredDate)} · ${item.preferredTime}\n(Preferensi pengguna bukan jadwal final.)",
                )
                LoopyInfo(
                    "Alamat & kontak",
                    "${item.pickupAddress}, ${item.city}\nTelp: ${item.phoneNumber}" +
                        if (item.notes.isNotBlank()) "\nCatatan: ${item.notes}" else "",
                )
            }
        }
    }
}
