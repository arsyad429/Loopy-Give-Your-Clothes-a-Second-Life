package id.ac.loopy.feature.home

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** S04 | F03/F09 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-87
 * TODO: Greeting; Check Your Clothes; donasi terakhir; saldo server; My Donations; Rewards; akun dan logout.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun HomeScreen(onBack: () -> Unit) {
    TemplateScreen("S04 · Home Dashboard", "Greeting; Check Your Clothes; donasi terakhir; saldo server; My Donations; Rewards; akun dan logout.", onBack)
}
