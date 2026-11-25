package id.ac.loopy.feature.rewards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.ac.loopy.domain.model.Reward
import id.ac.loopy.ui.components.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun RewardsScreen(
    state: RewardsUiState, onNavigate: (String) -> Unit, onAccount: () -> Unit,
    onSelect: (Reward) -> Unit, onRedeem: () -> Unit, onRetry: () -> Unit,
    onDismiss: () -> Unit, onResumePending: () -> Unit,
) {
    var history by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.message) { if (state.message != null) history = true }
    LoopyScreen("Rewards", onAccount = onAccount, bottomBar = { UserBottomBar("s10", onNavigate) }) {
        state.balance?.let { PointsCard(it) }
            ?: LoopyInfo("LOOPY POINTS", "Saldo belum tersedia.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RewardTab("Katalog", !history, Modifier.weight(1f)) { history = false }
            RewardTab("Reward-ku", history, Modifier.weight(1f)) { history = true }
        }
        if (state.loading) Text("Memuat reward…", style = MaterialTheme.typography.bodySmall)
        state.message?.let { LoopyInfo("Berhasil", it) }
        state.error?.let { LoopyInfo("Perlu diperbarui", it, isError = true) }
        state.redeemError?.let { LoopyInfo("Status penukaran", it, isError = true) }
        if (state.pending) {
            LoopyInfo("Penukaran tertunda", "Periksa hasil permintaan sebelumnya sebelum menukar reward lain.")
            LoopyButton("Periksa penukaran tertunda", onResumePending, enabled = !state.busy)
        }
        if (history) {
            Text("Reward-ku", style = MaterialTheme.typography.headlineMedium)
            Text("Riwayat penukaran digital", style = MaterialTheme.typography.bodySmall)
            if (state.loaded && state.redemptions.isEmpty()) LoopyInfo("Belum ada reward", "Reward yang berhasil ditukar akan muncul di sini.")
            state.redemptions.forEach { item ->
                LoopyCard(color = pointsCardColor()) {
                    LoopyAsset(item.icon, 44)
                    Text(item.rewardName, style = MaterialTheme.typography.titleLarge)
                    Text("Ditukar ${serverRewardDate(item.redeemedAt)} · ${item.pointsUsed} poin", style = MaterialTheme.typography.bodySmall)
                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                }
            }
            LoopyButton("Kembali ke katalog", { history = false }, secondary = true)
        } else {
            Text("Kebaikan yang\nkembali padamu.", style = MaterialTheme.typography.headlineMedium)
            if (state.loaded && state.rewards.isEmpty()) LoopyInfo("Belum ada reward", "Katalog reward belum tersedia.")
            state.rewards.forEach { item ->
                LoopyCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LoopyAsset(item.icon, 32)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.name, style = MaterialTheme.typography.labelLarge)
                            Text("${item.requiredPoints} poin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                    val enough = (state.balance ?: -1) >= item.requiredPoints
                    LoopyButton(
                        when {
                            !item.isActive -> "Tidak tersedia"
                            state.balance == null -> "Saldo belum tersedia"
                            enough -> "Tukar ${item.requiredPoints} poin"
                            else -> "Butuh ${item.requiredPoints - (state.balance ?: 0)} poin lagi"
                        },
                        onClick = { onSelect(item) },
                        enabled = item.isActive && enough && state.loaded && !state.loading && !state.busy && !state.pending,
                    )
                }
            }
        }
        LoopyButton("Muat ulang", onRetry, enabled = !state.busy && !state.loading, secondary = true)
        state.selected?.let { selected ->
            Dialog(onDismissRequest = { if (!state.busy) onDismiss() }) {
                LoopyCard {
                    LoopyAsset("gift", 40)
                    Text(if (state.pending) "Periksa penukaran?" else "Tukar ${selected.requiredPoints} poin?", style = MaterialTheme.typography.headlineMedium)
                    Text(selected.name, style = MaterialTheme.typography.bodyMedium)
                    state.balance?.let { Text("Saldo terakhir: $it poin", style = MaterialTheme.typography.bodyMedium) }
                    state.redeemError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    LoopyButton(
                        if (state.busy) "Memproses…" else if (state.pending) "Periksa hasil penukaran" else "Konfirmasi penukaran",
                        onRedeem, enabled = !state.busy && !state.loading &&
                            (state.pending || state.loaded && (state.balance ?: -1) >= selected.requiredPoints),
                    )
                    LoopyButton(if (state.pending) "Tutup" else "Batal", onDismiss, enabled = !state.busy, secondary = true)
                }
            }
        }
    }
}

// Go emits RFC3339Nano. Drop fractional seconds for this minute-resolution label.
internal fun serverRewardDate(raw: String): String = runCatching {
    val normalized = raw.replace(Regex("\\.\\d+(?=Z|[+-]\\d{2}:\\d{2}$)"), "")
    val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT).apply { isLenient = false }.parse(normalized)
    SimpleDateFormat("d MMM yyyy · HH.mm 'WIB'", Locale.forLanguageTag("id-ID")).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }.format(requireNotNull(date))
}.getOrDefault(raw)

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
