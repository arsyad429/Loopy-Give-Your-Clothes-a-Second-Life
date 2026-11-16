package id.ac.loopy.feature.pickup

import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.TemplateScreen

/** S07 | F06 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-90
 * TODO: Nama akun; telepon; alamat; kota; foto/jenis dari check; quantity >= 1; tanggal tidak lampau; preferensi waktu; notes opsional; Pending Review; konfirmasi sukses.
 * Ganti placeholder dengan UI stateless (state + callback); bisnis dikerjakan ViewModel/repository.
 */
@Composable
fun PickupRequestScreen(onBack: () -> Unit) {
    TemplateScreen("S07 · Pickup Request Form", "Nama akun; telepon; alamat; kota; foto/jenis dari check; quantity >= 1; tanggal tidak lampau; preferensi waktu; notes opsional; Pending Review; konfirmasi sukses.", onBack)
}
