package id.ac.loopy.feature.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import id.ac.loopy.ui.components.LoopyButton
import id.ac.loopy.ui.components.LoopyScreen

/** S04 | F03/F09 | https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-87
 * Menyediakan akses ke pemeriksaan pakaian untuk pengguna yang sudah masuk.
 */
@Composable
fun HomeScreen(name: String, onBack: () -> Unit, onCheckClothing: () -> Unit) {
    LoopyScreen(title = "Loopy", onBack = onBack) {
        Text("Halo, $name")
        LoopyButton("Periksa pakaian", onClick = onCheckClothing)
    }
}
