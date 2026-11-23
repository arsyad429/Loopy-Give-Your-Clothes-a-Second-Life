package id.ac.loopy.feature.rewards

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import id.ac.loopy.data.local.DemoDonation
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.feature.admin.DonationReview
import id.ac.loopy.ui.components.*

@Composable
fun CompletionAdminScreen(state: CompletionUiState, onBack: () -> Unit, onOpen: (String) -> Unit, onRetry: () -> Unit) {
    LoopyScreen("Penyelesaian donasi", onBack) {
        LoopyIntro("Konfirmasi langkah baik.", "Mode demo admin · perubahan disimpan lokal di Room, bukan dikirim ke server.")
        CompletionFeedback(state, onRetry)
        val active = state.donations.filter { it.status in listOf("PICKUP_SCHEDULED", "PICKED_UP") }
        val completed = state.donations.filter { it.status == "COMPLETED" }
        if (active.isEmpty() && !state.loading) LoopyInfo("Tidak ada penjemputan aktif", "Donasi yang sudah selesai tersedia di riwayat.")
        active.forEach { item -> DonationCard(item) { onOpen(item.id) } }
        Text("Donasi selesai", style = MaterialTheme.typography.titleLarge)
        completed.forEach { item -> DonationCard(item) { onOpen(item.id) } }
    }
}

internal fun DemoDonation.toAdminReview() = DonationReview(
    id = id, submittedAt = "Data demo", donor = "Arsyad Setiawan", city = "Malang", phone = "0812 0000 0000",
    address = address, clothing = "Kaus", quantity = quantity, assessment = "Berpotensi layak",
    assessmentDescription = "Pemeriksaan awal ilustrasi. Penerimaan akhir dikonfirmasi admin.", preference = "Pagi",
    notes = "Data simulasi alur penyelesaian donasi.", status = DonationStatus.valueOf(status), schedule = schedule,
)
