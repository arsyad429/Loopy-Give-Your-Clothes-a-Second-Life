package id.ac.loopy.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.ac.loopy.feature.rewards.CompletionUiState
import id.ac.loopy.ui.components.*

/** S04: Figma layout with the shared local demo balance and latest donation. */
@Composable
fun HomeScreen(
    state: CompletionUiState, onNavigate: (String) -> Unit,
    onDonation: (String) -> Unit, onCheck: () -> Unit, onAccount: () -> Unit, onRetry: () -> Unit,
) {
    LoopyScreen("loopy ↻", onAccount = onAccount, contentSpacing = 12, bottomBar = { UserBottomBar("s04", onNavigate) }) {
        Text("Hai, Arsyad!", style = MaterialTheme.typography.bodySmall)
        Text("Lemari lega.\nKebaikan bertambah.", style = MaterialTheme.typography.headlineMedium)
        LoopyCard(color = MaterialTheme.colorScheme.primary, padding = 16, spacing = 8) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LoopyAsset("shirt_hero", 48, MaterialTheme.colorScheme.onPrimary)
                Text("SECOND LIFE CLUB", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
            }
            Text("Punya baju yang\njarang dipakai?", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium)
            Text("Cek dulu, lalu ajukan penjemputan.", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodySmall)
            androidx.compose.material3.Button(
                onClick = onCheck, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
            ) { Text("Cek pakaian") }
        }
        PointsCard(state.balance) { onNavigate("s10") }
        CompletionFeedback(state, onRetry)
        Text("Donasi terakhirmu", style = MaterialTheme.typography.titleLarge)
        state.donations.firstOrNull()?.let { DonationCard(it) { onDonation(it.id) } }
        if (!state.loading && state.donations.isEmpty()) LoopyInfo("Belum ada donasi", "Pengajuanmu akan muncul di sini.")
        Text("Tip: pakaian bersih, kering, dan aman dipakai.", style = MaterialTheme.typography.bodySmall)
        Text("Demo lokal · belum terhubung ke server.", style = MaterialTheme.typography.bodySmall)
    }
}

/** S04 | F03/F09 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-87
 * Menyediakan akses ke pemeriksaan pakaian untuk pengguna yang sudah masuk.
 */
@Composable
fun HomeScreen(
    name: String,
    onBack: () -> Unit,
    onCheckClothing: () -> Unit,
    onMyDonations: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    logoutBusy: Boolean = false,
    logoutError: String? = null,
) {
    LoopyScreen(title = "Loopy", onBack = onBack) {
        Text("Halo, $name", style = MaterialTheme.typography.headlineMedium)
        LoopyButton("Periksa pakaian", onClick = onCheckClothing)
        if (onMyDonations != null) {
            LoopyButton("Donasiku", onClick = onMyDonations, outlined = true)
        }
        logoutError?.let { LoopyInfo("Gagal keluar", it, isError = true) }
        if (onLogout != null) {
            LoopyButton(if (logoutBusy) "Mengakhiri sesi…" else "Keluar", onLogout, enabled = !logoutBusy, outlined = true)
        }
    }
}
