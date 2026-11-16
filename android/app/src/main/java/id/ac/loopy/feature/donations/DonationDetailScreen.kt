package id.ac.loopy.feature.donations

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** S09 | F08 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-94
 * TODO: Timeline lima status; jadwal preferensi dan jadwal final dibedakan; alasan rejected; foto/detail; pengguna tidak dapat mengubah status.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun DonationDetailScreen(onBack: () -> Unit) {
    TemplateScreen("S09 · Donation Detail", "Timeline lima status; jadwal preferensi dan jadwal final dibedakan; alasan rejected; foto/detail; pengguna tidak dapat mengubah status.", onBack)
}
