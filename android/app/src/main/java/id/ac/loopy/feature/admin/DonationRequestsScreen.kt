package id.ac.loopy.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.ui.components.LoopyInfo
import id.ac.loopy.ui.components.LoopyIntro
import id.ac.loopy.ui.components.LoopyScreen
import id.ac.loopy.ui.components.LoopyTextField
import id.ac.loopy.ui.components.StatusBadge

private enum class RequestFilter(val label: String, val statuses: Set<DonationStatus>) {
    Pending("Pending", setOf(DonationStatus.PENDING_REVIEW)),
    Scheduled("Terjadwal", setOf(DonationStatus.PICKUP_SCHEDULED, DonationStatus.PICKED_UP)),
    Completed("Selesai", setOf(DonationStatus.COMPLETED)),
    Rejected("Ditolak", setOf(DonationStatus.REJECTED)),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DonationRequestsScreen(
    reviews: List<DonationReview>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(RequestFilter.Pending) }
    val needle = query.trim()

    val visibleReviews = reviews.filter { review ->
        review.status in filter.statuses &&
            listOf(review.id, "#${review.id}", review.donor, review.city).any { it.contains(needle, ignoreCase = true) }
    }

    LoopyScreen(title = "Pengajuan", onBack = onBack) {
        LoopyIntro("Antrean donasi", "Periksa pengajuan yang paling lama menunggu.")

        LoopyTextField(
            label = "Cari pengajuan",
            value = query,
            onValueChange = { query = it },
            placeholder = "ID, nama, atau kota",
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RequestFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    label = { Text(option.label) },
                )
            }
        }

        Text("${visibleReviews.size} pengajuan", style = MaterialTheme.typography.labelLarge)

        if (visibleReviews.isEmpty()) {
            LoopyInfo("Belum ada hasil", "Coba kata kunci atau filter yang lain.")
        }

        visibleReviews.forEach { review ->
            Card(
                onClick = { onOpen(review.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("#${review.id} · ${review.donor}", style = MaterialTheme.typography.titleMedium)
                    Text("${review.quantity} pakaian · ${review.city}", style = MaterialTheme.typography.bodyMedium)
                    Text("Diajukan ${review.submittedAt}", style = MaterialTheme.typography.bodySmall)
                    StatusBadge(review.status)
                }
            }
        }
    }
}
