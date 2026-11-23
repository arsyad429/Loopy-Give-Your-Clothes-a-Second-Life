package id.ac.loopy.feature.donations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import id.ac.loopy.feature.rewards.CompletionUiState
import id.ac.loopy.ui.components.*

/** S08: local demo list and status filters supporting the completion flow. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun MyDonationsScreen(
    state: CompletionUiState, onNavigate: (String) -> Unit,
    onOpen: (String) -> Unit, onAccount: () -> Unit, onRetry: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("Semua") }
    val items = state.donations.filter {
        when (filter) {
            "Aktif" -> it.status !in listOf("COMPLETED", "REJECTED")
            "Selesai" -> it.status == "COMPLETED"
            "Ditolak" -> it.status == "REJECTED"
            else -> true
        }
    }
    LoopyScreen("Donasiku", onAccount = onAccount, bottomBar = { UserBottomBar("s08", onNavigate) }) {
        LoopyIntro("Setiap donasi\npunya cerita.", "Pantau perjalanan pakaian yang kamu bagikan.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Semua", "Aktif", "Selesai", "Ditolak").forEach { label ->
                FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) }, shape = CircleShape, border = null,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface, labelColor = MaterialTheme.colorScheme.onSurfaceVariant))
            }
        }
        CompletionFeedback(state, onRetry)
        if (!state.loading && items.isEmpty()) LoopyInfo("Belum ada hasil", "Tidak ada donasi pada filter ini.")
        val active = items.filter { it.status !in listOf("COMPLETED", "REJECTED") }
        val history = items.filter { it.status in listOf("COMPLETED", "REJECTED") }
        if (active.isNotEmpty()) Text("Pengajuan aktif", style = MaterialTheme.typography.titleLarge)
        active.forEach { item -> DonationCard(item) { onOpen(item.id) } }
        if (history.isNotEmpty()) Text("Riwayat donasi", style = MaterialTheme.typography.titleLarge)
        history.forEach { item -> DonationCard(item) { onOpen(item.id) } }
        Text("Demo lokal · belum terhubung ke server.", style = MaterialTheme.typography.bodySmall)
    }
}
