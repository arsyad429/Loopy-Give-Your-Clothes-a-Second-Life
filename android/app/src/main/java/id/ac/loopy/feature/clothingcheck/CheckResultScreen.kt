package id.ac.loopy.feature.clothingcheck

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** S06 | F05 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-89
 * TODO: Foto, jenis, hasil, deskripsi; tiga kategori; eligible saja boleh pickup; konfirmasi pakaian bersih dan aman; hasil AI estimasi awal.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun CheckResultScreen(onBack: () -> Unit) {
    TemplateScreen("S06 · Clothing Check Result", "Foto, jenis, hasil, deskripsi; tiga kategori; eligible saja boleh pickup; konfirmasi pakaian bersih dan aman; hasil AI estimasi awal.", onBack)
}
