package id.ac.loopy.feature.admin

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** A03 | F07/F08/F09 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-100
 * TODO: Foto + hasil AI + alamat; accept wajib jadwal; reject wajib alasan; Pending → Scheduled → Picked Up → Completed; audit history; completed +10 sekali secara atomik di backend.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun AdminDonationDetailScreen(onBack: () -> Unit) {
    TemplateScreen("A03 · Admin Donation Detail", "Foto + hasil AI + alamat; accept wajib jadwal; reject wajib alasan; Pending → Scheduled → Picked Up → Completed; audit history; completed +10 sekali secara atomik di backend.", onBack)
}
