package id.ac.loopy.feature.donations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.ui.components.*
import java.util.Locale

/** S08 (Akun aktif): daftar pengajuan donasi pengguna dari backend Go. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun MyDonationsScreen(
    viewModel: MyDonationsViewModel,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onCheckClothing: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val items = state.filteredItems

    LoopyScreen(title = "Donasiku", onBack = onBack) {
        LoopyIntro("Setiap donasi\npunya cerita.", "Pantau perjalanan pakaian yang kamu bagikan.")

        LoopyTextField(
            label = "Cari donasi",
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            placeholder = "Cari kota atau ID pengajuan",
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DonationListFilter.entries.forEach { option ->
                FilterChip(
                    selected = state.filter == option,
                    onClick = { viewModel.onFilterChange(option) },
                    label = { Text(option.label) },
                    shape = CircleShape,
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }

        when {
            state.isLoading -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Memuat daftar donasimu…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            state.error != null -> {
                LoopyInfo("Belum berhasil", state.error.orEmpty(), isError = true)
                LoopyButton("Muat ulang", viewModel::refresh)
            }
            items.isEmpty() -> {
                LoopyInfo(
                    title = "Belum ada donasi",
                    description = "Periksa pakaian dengan AI lalu ajukan penjemputan untuk melihat daftar donasimu.",
                )
                LoopyButton("Periksa pakaian", onCheckClothing)
            }
            else -> {
                val active = items.filter { it.status != DonationStatus.COMPLETED && it.status != DonationStatus.REJECTED }
                val history = items.filter { it.status == DonationStatus.COMPLETED || it.status == DonationStatus.REJECTED }
                if (active.isNotEmpty()) {
                    Text("Pengajuan aktif", style = MaterialTheme.typography.titleLarge)
                    active.forEach { item -> DonationCard(item) { onOpen(item.donationId) } }
                }
                if (history.isNotEmpty()) {
                    Text("Riwayat donasi", style = MaterialTheme.typography.titleLarge)
                    history.forEach { item -> DonationCard(item) { onOpen(item.donationId) } }
                }
                LoopyButton("Perbarui daftar", viewModel::refresh, outlined = true)
            }
        }
    }
}
