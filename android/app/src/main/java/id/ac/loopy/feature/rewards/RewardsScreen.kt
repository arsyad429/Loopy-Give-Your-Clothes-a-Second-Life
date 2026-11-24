package id.ac.loopy.feature.rewards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.ac.loopy.data.repository.DemoRewardCatalog
import id.ac.loopy.ui.components.*
import java.util.UUID

/** S10, RedeemConfirm and Reward-ku; the shared state is persisted in Room. */
@Composable
fun RewardsScreen(
    state: CompletionUiState, onNavigate: (String) -> Unit,
    onAccount: () -> Unit, onRedeem: (String, String) -> Unit, onRetry: () -> Unit, onDismissFeedback: () -> Unit,
) {
    var history by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var requestKey by rememberSaveable { mutableStateOf("") }
    val selected = DemoRewardCatalog.find { it.id == selectedId }
    LaunchedEffect(state.redemptions, requestKey) {
        if (requestKey.isNotBlank() && state.redemptions.any { it.requestKey == requestKey }) {
            selectedId = null
            history = true
        }
    }
    LoopyScreen("Rewards", onAccount = onAccount, bottomBar = { UserBottomBar("s10", onNavigate) }) {
        PointsCard(state.balance)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RewardTab("Katalog", !history, Modifier.weight(1f)) { history = false; onDismissFeedback() }
            RewardTab("Reward-ku", history, Modifier.weight(1f)) { history = true }
        }
        CompletionFeedback(state, onRetry)
        if (history) {
            Text("Reward-ku", style = MaterialTheme.typography.headlineMedium)
            Text("Riwayat penukaran digital", style = MaterialTheme.typography.bodySmall)
            if (!state.loading && state.redemptions.isEmpty()) LoopyInfo("Belum ada reward", "Reward yang berhasil ditukar akan muncul di sini.")
            state.redemptions.forEach { item ->
                LoopyCard(color = pointsCardColor()) {
                    LoopyAsset(item.icon, 44)
                    Text(item.name, style = MaterialTheme.typography.titleLarge)
                    Text("Ditukar ${rewardDate(item.redeemedAt)} · ${item.pointsUsed} poin", style = MaterialTheme.typography.bodySmall)
                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                }
            }
            LoopyButton("Kembali ke katalog", { history = false; onDismissFeedback() }, secondary = true)
        } else {
            Text("Kebaikan yang\nkembali padamu.", style = MaterialTheme.typography.headlineMedium)
            DemoRewardCatalog.forEach { item ->
                LoopyCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LoopyAsset(item.icon, 32)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.name, style = MaterialTheme.typography.labelLarge)
                            Text("${item.points} poin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                    val enough = state.balance >= item.points
                    LoopyButton(
                        if (enough) "Tukar ${item.points} poin" else "Butuh ${item.points - state.balance} poin lagi",
                        onClick = { selectedId = item.id; requestKey = UUID.randomUUID().toString() },
                        enabled = enough && !state.loading && !state.busy,
                    )
                }
            }
        }
        Text("Demo lokal · data tersimpan di HP. Belum terhubung ke server.", style = MaterialTheme.typography.bodySmall)
        if (selected != null) {
            Dialog(onDismissRequest = { if (!state.busy) selectedId = null }) {
                LoopyCard {
                    LoopyAsset("gift", 40)
                    Text("Tukar ${selected.points} poin?", style = MaterialTheme.typography.headlineMedium)
                    Text("${selected.name}\nSaldo sekarang: ${state.balance} poin\nSaldo setelah penukaran: ${(state.balance - selected.points).coerceAtLeast(0)} poin", style = MaterialTheme.typography.bodyMedium)
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    LoopyButton(if (state.busy) "Memproses…" else "Konfirmasi penukaran", { onRedeem(selected.id, requestKey) }, enabled = !state.busy && state.balance >= selected.points)
                    LoopyButton("Batal", { selectedId = null }, enabled = !state.busy, secondary = true)
                }
            }
        }
    }
}

@Composable
private fun RewardTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    androidx.compose.material3.Button(
        onClick = onClick, modifier = modifier.heightIn(min = 44.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) { Text(label, style = MaterialTheme.typography.labelMedium) }
}
