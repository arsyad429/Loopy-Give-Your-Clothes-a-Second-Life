package id.ac.loopy.feature.auth

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** S03 | F02 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-84
 * TODO: Nama/email/password; email unik; role user otomatis; saldo awal 0; admin tidak melalui registrasi publik.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun RegisterScreen(onBack: () -> Unit) {
    TemplateScreen("S03 · Register", "Nama/email/password; email unik; role user otomatis; saldo awal 0; admin tidak melalui registrasi publik.", onBack)
}
