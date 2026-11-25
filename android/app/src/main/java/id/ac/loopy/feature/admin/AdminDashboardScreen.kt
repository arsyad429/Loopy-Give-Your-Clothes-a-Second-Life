package id.ac.loopy.feature.admin

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import id.ac.loopy.ui.components.AdminBottomBar
import id.ac.loopy.ui.components.LoopyButton
import id.ac.loopy.ui.components.LoopyInfo
import id.ac.loopy.ui.components.LoopyScreen

@Composable
internal fun AdminDashboardScreen(
    model: AdminDashboardViewModel,
    name: String,
    onOpenRequests: () -> Unit,
    onOpenDonation: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onAccount: () -> Unit,
    onLogout: () -> Unit,
    logoutBusy: Boolean,
    logoutError: String?,
) {
    val state by model.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refresh() }

    LoopyScreen(
        title = "Dashboard admin",
        onAccount = onAccount,
        accountInitial = name.take(1).uppercase(),
        bottomBar = { AdminBottomBar("a01", onNavigate) },
        footer = {
            LoopyButton(if (logoutBusy) "Mengakhiri sesi…" else "Keluar", onLogout, enabled = !logoutBusy, outlined = true)
        },
    ) {
        Text("Halo, $name", style = MaterialTheme.typography.headlineMedium)
        state.summary?.let { summary ->
            Text("${summary.total} pengajuan", style = MaterialTheme.typography.titleLarge)
            LoopyInfo("Menunggu verifikasi", "${summary.pending} pengajuan")
            LoopyInfo("Penjemputan", "${summary.scheduled} terjadwal · ${summary.pickedUp} dijemput")
            LoopyInfo("Riwayat", "${summary.completed} selesai · ${summary.rejected} ditolak")
        }
        if (state.loading) CircularProgressIndicator()
        state.error?.let {
            LoopyInfo("Ringkasan belum tersedia", it, isError = true)
            LoopyButton("Muat ulang ringkasan", model::refresh, outlined = true)
        }
        state.pending?.let { pending ->
            Text("Perlu ditangani", style = MaterialTheme.typography.titleLarge)
            if (pending.isEmpty()) {
                LoopyInfo("Antrean kosong", "Belum ada pengajuan yang menunggu verifikasi.")
            }
            pending.forEach { item ->
                DonationQueueCard(item.donationId, item.donorName, item.quantity, item.city, formatAdminTimestamp(item.createdAt), item.status) {
                    onOpenDonation(item.donationId)
                }
            }
        }
        logoutError?.let { LoopyInfo("Gagal keluar", it, isError = true) }
        LoopyButton("Buka antrean donasi", onOpenRequests, enabled = !logoutBusy)
    }
}
