package id.ac.loopy.feature.clothingcheck

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** S05 | F04 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-88
 * TODO: Photo Picker dan kamera Activity Result + FileProvider; validasi MIME/ukuran/kejelasan; upload ke backend AI; loading/error/retry; gagal tidak boleh lanjut.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun ClothingDetectorScreen(onBack: () -> Unit) {
    TemplateScreen("S05 · AI Clothing Detector", "Photo Picker dan kamera Activity Result + FileProvider; validasi MIME/ukuran/kejelasan; upload ke backend AI; loading/error/retry; gagal tidak boleh lanjut.", onBack)
}
