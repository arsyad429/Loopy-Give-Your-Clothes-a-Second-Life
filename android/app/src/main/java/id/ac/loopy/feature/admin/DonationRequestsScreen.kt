package id.ac.loopy.feature.admin

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** A02 | F07 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-99
 * TODO: Guard admin; daftar/filter/pencarian requests; empty/error/loading; buka detail dengan ID.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun DonationRequestsScreen(onBack: () -> Unit) {
    TemplateScreen("A02 · Donation Requests", "Guard admin; daftar/filter/pencarian requests; empty/error/loading; buka detail dengan ID.", onBack)
}
