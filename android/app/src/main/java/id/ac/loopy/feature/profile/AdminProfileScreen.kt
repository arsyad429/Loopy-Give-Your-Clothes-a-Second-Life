package id.ac.loopy.feature.profile

import androidx.compose.runtime.Composable

@Composable
internal fun AdminProfileScreen(
    model: AccountViewModel,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onDashboard: () -> Unit,
    onLogout: () -> Unit,
    logoutBusy: Boolean,
    logoutError: String?,
) = AccountProfileScreen(
    model, "Profil admin", "Buka dashboard", onBack, onSettings, onDashboard, onLogout, logoutBusy, logoutError,
)
