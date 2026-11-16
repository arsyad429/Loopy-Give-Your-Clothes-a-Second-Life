package id.ac.loopy.feature.rewards

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** S10 | F09/F10 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-95
 * TODO: Saldo server; katalog 10/20/30 poin; tombol disable jika tidak cukup; dialog konfirmasi; idempotency; riwayat My Rewards; voucher berlabel demo.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun RewardsScreen(onBack: () -> Unit) {
    TemplateScreen("S10 · Rewards & My Rewards", "Saldo server; katalog 10/20/30 poin; tombol disable jika tidak cukup; dialog konfirmasi; idempotency; riwayat My Rewards; voucher berlabel demo.", onBack)
}
