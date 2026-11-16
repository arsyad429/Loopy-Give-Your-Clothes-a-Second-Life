package id.ac.loopy.feature.profile

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** A04 | Design extension | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=19-498
 * TODO: Informasi admin/logout; jangan menyediakan eskalasi role dari UI.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun AdminProfileScreen(onBack: () -> Unit) {
    TemplateScreen("A04 · Profil Admin", "Informasi admin/logout; jangan menyediakan eskalasi role dari UI.", onBack)
}
