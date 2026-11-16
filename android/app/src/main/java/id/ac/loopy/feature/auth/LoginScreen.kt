package id.ac.loopy.feature.auth

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** S02 | F02 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-83
 * TODO: Email/password; validasi; pesan gagal; loading; login user → Home, admin → Admin Dashboard; cegah double submit.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun LoginScreen(onBack: () -> Unit) {
    TemplateScreen("S02 · Login", "Email/password; validasi; pesan gagal; loading; login user → Home, admin → Admin Dashboard; cegah double submit.", onBack)
}
