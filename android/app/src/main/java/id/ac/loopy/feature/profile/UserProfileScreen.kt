package id.ac.loopy.feature.profile

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.loopy.ui.components.LoopyButton
import id.ac.loopy.ui.components.LoopyCard
import id.ac.loopy.ui.components.LoopyInfo
import id.ac.loopy.ui.components.LoopyScreen
import id.ac.loopy.ui.components.PointsCard

@Composable
internal fun UserProfileScreen(
    model: AccountViewModel,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onRewards: () -> Unit,
    onLogout: () -> Unit,
    logoutBusy: Boolean,
    logoutError: String?,
) = AccountProfileScreen(
    model, "Profil kamu", "Lihat Rewards", onBack, onSettings, onRewards, onLogout, logoutBusy, logoutError,
)

@Composable
internal fun AccountProfileScreen(
    model: AccountViewModel,
    title: String,
    actionLabel: String,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onAction: () -> Unit,
    onLogout: () -> Unit,
    logoutBusy: Boolean,
    logoutError: String?,
) {
    val state by model.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refresh() }

    LoopyScreen(title = title, onBack = onBack, footer = {
        LoopyButton(if (logoutBusy) "Mengakhiri sesi…" else "Keluar dari akun", onLogout, enabled = !logoutBusy, outlined = true)
    }) {
        state.profile?.let { profile ->
            LoopyCard(spacing = 8) {
                Text(if (profile.role == "ADMIN") "Admin" else "Donatur", style = MaterialTheme.typography.labelMedium)
                Text(profile.name, style = MaterialTheme.typography.headlineMedium)
                profile.username?.takeIf { it.isNotBlank() }?.let { Text("@$it") }
                Text(profile.email)
                Text(if (profile.emailVerified) "Email terverifikasi" else "Email belum diverifikasi", style = MaterialTheme.typography.bodySmall)
            }
            PointsCard(profile.totalPoints)
            LoopyButton("Pengaturan akun", onSettings)
            LoopyButton(actionLabel, onAction, outlined = true)
        }
        if (state.loading) CircularProgressIndicator()
        state.error?.let {
            LoopyInfo("Profil belum tersedia", it, isError = true)
            LoopyButton("Muat ulang", model::refresh, outlined = true)
        }
        logoutError?.let { LoopyInfo("Gagal keluar", it, isError = true) }
    }
}
