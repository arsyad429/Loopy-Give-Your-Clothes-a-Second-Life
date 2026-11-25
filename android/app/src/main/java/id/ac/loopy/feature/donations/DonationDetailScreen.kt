package id.ac.loopy.feature.donations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.domain.model.DonationStatusHistory
import id.ac.loopy.feature.pickup.PickupRequestValidator
import id.ac.loopy.ui.components.*
import java.util.Locale

/** S09 (Akun aktif): detail & timeline status donasi dari backend Go. */
@Composable
fun DonationDetailScreen(
    viewModel: DonationDetailViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoopyScreen("Detail donasi", onBack, bottomBar = { UserBottomBar("s08", onNavigate) }) {
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
                DonationTimeline(item.status, item.history)
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

@Composable
private fun DonationTimeline(status: DonationStatus, history: List<DonationStatusHistory>) {
    val steps = if (status == DonationStatus.REJECTED) {
        listOf(DonationStatus.PENDING_REVIEW, DonationStatus.REJECTED)
    } else {
        listOf(DonationStatus.PENDING_REVIEW, DonationStatus.PICKUP_SCHEDULED, DonationStatus.PICKED_UP, DonationStatus.COMPLETED)
    }

    val colors = MaterialTheme.colorScheme
    val currentStep = steps.indexOf(status)
    LoopyCard(spacing = 12) {
        Text("Perjalanan donasi", style = MaterialTheme.typography.titleLarge)
        Column {
            steps.forEachIndexed { index, step ->
                val event = history.lastOrNull { it.newStatus == step }
                val reached = event != null || index <= currentStep
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val icon = when (step) {
                        DonationStatus.PENDING_REVIEW -> "check"
                        DonationStatus.PICKUP_SCHEDULED -> "calendar"
                        DonationStatus.PICKED_UP -> "package"
                        DonationStatus.COMPLETED -> "gift"
                        DonationStatus.REJECTED -> "package"
                    }
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier.size(34.dp).background(
                                if (reached) colors.primaryContainer else colors.surfaceVariant,
                                CircleShape,
                            ),
                            contentAlignment = Alignment.Center,
                        ) { LoopyAsset(icon, 18) }
                        if (index < steps.lastIndex) {
                            Box(Modifier.weight(1f).width(2.dp).background(colors.outlineVariant))
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(bottom = if (index < steps.lastIndex) 14.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        val label = when (step) {
                            DonationStatus.PENDING_REVIEW -> "Pengajuan terkirim"
                            DonationStatus.PICKUP_SCHEDULED -> "Penjemputan dijadwalkan"
                            DonationStatus.PICKED_UP -> "Pakaian dijemput"
                            DonationStatus.COMPLETED -> "Donasi selesai"
                            DonationStatus.REJECTED -> "Pengajuan ditolak"
                        }
                        Text(label, style = MaterialTheme.typography.labelLarge)
                        Text(
                            event?.updatedAt?.let(::formatDonationTimestamp) ?: if (reached) "Tercatat" else "Menunggu",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
