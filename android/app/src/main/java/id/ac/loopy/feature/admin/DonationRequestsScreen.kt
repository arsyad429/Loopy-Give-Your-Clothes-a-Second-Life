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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.ui.components.AdminBottomBar
import id.ac.loopy.ui.components.LoopyButton
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
            if (needle.isEmpty()) {
                LoopyInfo("Belum ada pengajuan", "Pilih status lain untuk melihat pengajuan lainnya.")
            } else {
                LoopyInfo("Pengajuan tidak ditemukan", "Coba kata kunci atau pilih status lain.")
            }
        }

        visibleReviews.forEach { review ->
            DonationQueueCard(review.id, review.donor, review.quantity, review.city, review.submittedAt, review.status) {
                onOpen(review.id)
            }
        }
    }
}

private val queueFilters = listOf(
    DonationStatus.PENDING_REVIEW to "Pending",
    DonationStatus.PICKUP_SCHEDULED to "Terjadwal",
    DonationStatus.PICKED_UP to "Dijemput",
    DonationStatus.COMPLETED to "Selesai",
    DonationStatus.REJECTED to "Ditolak",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DonationRequestsScreen(
    model: DonationRequestsViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onAccount: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refresh() }

    LoopyScreen(
        title = "Pengajuan",
        onBack = onBack,
        onAccount = onAccount,
        bottomBar = { AdminBottomBar("a02", onNavigate) },
    ) {
        LoopyIntro("Antrean donasi", "Periksa pengajuan yang paling lama menunggu.")
        LoopyTextField("Cari pengajuan", state.query, model::search, placeholder = "ID, nama, atau kota")

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            queueFilters.forEach { (status, label) ->
                FilterChip(selected = state.status == status, onClick = { model.filter(status) }, label = { Text(label) })
            }
        }

        state.items.forEach { item ->
            DonationQueueCard(item.donationId, item.donorName, item.quantity, item.city, formatAdminTimestamp(item.createdAt), item.status) {
                onOpen(item.donationId)
            }
        }
        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> {
                LoopyInfo("Antrean belum tersedia", state.error.orEmpty(), isError = true)
                LoopyButton("Coba lagi", model::retry, outlined = true)
            }
            state.items.isEmpty() -> LoopyInfo("Belum ada pengajuan", "Coba kata kunci atau pilih status lain.")
            state.nextCursor != null -> LoopyButton("Muat pengajuan berikutnya", model::loadMore, outlined = true)
        }
    }
}

@Composable
internal fun DonationQueueCard(
    id: String,
    donor: String,
    quantity: Int,
    city: String,
    submittedAt: String,
    status: DonationStatus,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("#$id · $donor", style = MaterialTheme.typography.titleMedium)
            Text("$quantity pakaian · $city", style = MaterialTheme.typography.bodyMedium)
            Text("Diajukan $submittedAt", style = MaterialTheme.typography.bodySmall)
            StatusBadge(status)
        }
    }
}
