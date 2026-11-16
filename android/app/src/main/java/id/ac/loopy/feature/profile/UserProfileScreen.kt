package id.ac.loopy.feature.profile

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** S11 | F03 extension | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=19-472
 * TODO: Informasi akun/logout. Penyuntingan profil lengkap perlu kesepakatan tim.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun UserProfileScreen(onBack: () -> Unit) {
    TemplateScreen("S11 · Profil User", "Informasi akun/logout. Penyuntingan profil lengkap perlu kesepakatan tim.", onBack)
}
