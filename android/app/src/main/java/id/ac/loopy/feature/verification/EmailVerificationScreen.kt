package id.ac.loopy.feature.verification

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** S13 | Design extension | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=19-511
 * TODO: OTP expiry/resend/rate limit/percobaan gagal; alur belum diwajibkan PRD 2.1, sepakati sebelum implementasi.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun EmailVerificationScreen(onBack: () -> Unit) {
    TemplateScreen("S13 · Verifikasi Email", "OTP expiry/resend/rate limit/percobaan gagal; alur belum diwajibkan PRD 2.1, sepakati sebelum implementasi.", onBack)
}
