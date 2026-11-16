package id.ac.loopy.feature.donations

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** S08 | F08 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-93
 * TODO: Daftar donasi user aktif saja; loading/empty/error; refresh status terbaru; klik ID ke detail; filter/pencarian reaktif Flow.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun MyDonationsScreen(onBack: () -> Unit) {
    TemplateScreen("S08 · My Donations", "Daftar donasi user aktif saja; loading/empty/error; refresh status terbaru; klik ID ke detail; filter/pencarian reaktif Flow.", onBack)
}
