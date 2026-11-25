package id.ac.loopy.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.loopy.feature.rewards.CompletionUiState
import id.ac.loopy.ui.components.CompletionFeedback
import id.ac.loopy.ui.components.DonationCard
import id.ac.loopy.ui.components.LoopyAsset
import id.ac.loopy.ui.components.LoopyButton
import id.ac.loopy.ui.components.LoopyCard
import id.ac.loopy.ui.components.LoopyInfo
import id.ac.loopy.ui.components.LoopyScreen
import id.ac.loopy.ui.components.PointsCard
import id.ac.loopy.ui.components.UserBottomBar

@Composable
internal fun HomeScreen(
    viewModel: HomeViewModel,
    name: String,
    onCheckClothing: () -> Unit,
    onMyDonations: () -> Unit,
    onRewards: () -> Unit,
    onDonation: (String) -> Unit,
    onLogout: () -> Unit,
    logoutBusy: Boolean,
    logoutError: String?,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    HomeContent(
        state = state,
        name = name,
        onCheckClothing = onCheckClothing,
        onMyDonations = onMyDonations,
        onRewards = onRewards,
        onDonation = onDonation,
        onRefresh = viewModel::refresh,
        onLogout = onLogout,
        logoutBusy = logoutBusy,
        logoutError = logoutError,
    )
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    name: String,
    onCheckClothing: () -> Unit,
    onMyDonations: () -> Unit,
    onRewards: () -> Unit,
    onDonation: (String) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    logoutBusy: Boolean = false,
    logoutError: String? = null,
) {
    var showAccount by rememberSaveable { mutableStateOf(false) }
    HomeLayout(
        name = name,
        onNavigate = { route ->
            when (route) {
                "s08" -> onMyDonations()
                "s10" -> onRewards()
            }
        },
        onAccount = { showAccount = true },
        onCheck = onCheckClothing,
    ) {
        if (state.points != null) {
            PointsCard(state.points, onRewards)
        } else {
            LoopyInfo(
                "Loopy Points",
                if (state.isLoading) "Memuat saldo…" else "Saldo gagal dimuat.",
                isError = !state.isLoading && state.pointsUnavailable,
            )
        }

        Text("Donasi terakhirmu", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        val donation = state.latestDonation
        when {
            donation != null -> DonationCard(donation) { onDonation(donation.donationId) }
            state.isLoading -> Text("Memuat donasi…", style = MaterialTheme.typography.bodySmall)
            state.donationUnavailable -> LoopyInfo("Donasi gagal dimuat", "Coba perbarui beranda.", isError = true)
            else -> LoopyInfo("Belum ada donasi", "Pengajuan pertamamu akan muncul di sini.")
        }

        if (state.isLoading && (state.points != null || donation != null)) {
            Text("Memperbarui beranda…", style = MaterialTheme.typography.bodySmall)
        } else if ((state.pointsUnavailable && state.points != null) || (state.donationUnavailable && donation != null)) {
            Text("Sebagian informasi menampilkan data terakhir yang berhasil dimuat.", style = MaterialTheme.typography.bodySmall)
        }
        val refreshLabel = if (state.pointsUnavailable || state.donationUnavailable) "Coba lagi" else "Perbarui beranda"
        LoopyButton(refreshLabel, onRefresh, enabled = !state.isLoading, outlined = true)
    }

    if (showAccount) {
        AlertDialog(
            onDismissRequest = { if (!logoutBusy) showAccount = false },
            title = { Text("Akun") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name)
                    if (logoutError != null) Text(logoutError, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = onLogout, enabled = !logoutBusy) {
                    Text(if (logoutBusy) "Sedang keluar…" else "Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccount = false }, enabled = !logoutBusy) { Text("Tutup") }
            },
        )
    }
}

/** S04 preview with the separate local completion demo data. */
@Composable
internal fun HomeScreen(
    state: CompletionUiState,
    onNavigate: (String) -> Unit,
    onDonation: (String) -> Unit,
    onCheck: () -> Unit,
    onAccount: () -> Unit,
    onRetry: () -> Unit,
) {
    HomeLayout(name = "Arsyad", onNavigate = onNavigate, onAccount = onAccount, onCheck = onCheck) {
        PointsCard(state.balance) { onNavigate("s10") }
        CompletionFeedback(state, onRetry)
        Text("Donasi terakhirmu", style = MaterialTheme.typography.titleLarge)
        state.donations.firstOrNull()?.let { DonationCard(it) { onDonation(it.id) } }
        if (!state.loading && state.donations.isEmpty()) LoopyInfo("Belum ada donasi", "Pengajuanmu akan muncul di sini.")
        Text("Demo lokal", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HomeLayout(
    name: String,
    onNavigate: (String) -> Unit,
    onAccount: () -> Unit,
    onCheck: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    LoopyScreen(
        title = "loopy ↻",
        onAccount = onAccount,
        contentSpacing = 12,
        accountInitial = name.trim().take(1).uppercase().ifEmpty { "U" },
        bottomBar = { UserBottomBar("s04", onNavigate) },
    ) {
        Text("Hai, $name!", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Lemari lega.\nKebaikan bertambah.",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        LoopyCard(color = MaterialTheme.colorScheme.primary, padding = 16, spacing = 8) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                LoopyAsset("shirt_hero", 48, MaterialTheme.colorScheme.onPrimary)
                Text("Punya baju yang jarang dipakai?", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
            }
            Text("Cek kondisinya sebelum mengajukan penjemputan.", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodySmall)
            LoopyButton("Cek pakaian", onCheck, secondary = true)
        }
        content()
        Text("Tip donasi. Pilih pakaian yang bersih, kering, dan aman dipakai.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}
