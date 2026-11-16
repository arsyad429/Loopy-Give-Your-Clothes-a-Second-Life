package id.ac.loopy.feature.admin

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** A01 | F07 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-98
 * TODO: Guard admin server; metrik total/pending/scheduled/completed/rejected; navigasi requests; logout.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun AdminDashboardScreen(onBack: () -> Unit) {
    TemplateScreen("A01 · Admin Dashboard", "Guard admin server; metrik total/pending/scheduled/completed/rejected; navigasi requests; logout.", onBack)
}
