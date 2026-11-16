package id.ac.loopy.feature.settings

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** S12 | Design extension | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=19-485
 * TODO: Preferensi non-sensitif DataStore; ganti password/username memerlukan kontrak backend dan persetujuan scope.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    TemplateScreen("S12 · Settings", "Preferensi non-sensitif DataStore; ganti password/username memerlukan kontrak backend dan persetujuan scope.", onBack)
}
