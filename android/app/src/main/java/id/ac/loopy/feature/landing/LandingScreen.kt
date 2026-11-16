package id.ac.loopy.feature.landing

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** S01 | F01 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-82
 * TODO: Hero dan tagline; About Loopy; tiga benefit; empat langkah; Get Started → Register; Login → Login.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun LandingScreen(onBack: () -> Unit) {
    TemplateScreen("S01 · Landing Page", "Hero dan tagline; About Loopy; tiga benefit; empat langkah; Get Started → Register; Login → Login.", onBack)
}
