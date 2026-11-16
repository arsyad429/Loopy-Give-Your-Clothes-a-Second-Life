package id.ac.loopy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import id.ac.loopy.ui.theme.LoopyTheme

// PAPB Preview: UI stateless dapat diperiksa tanpa ViewModel, backend, atau emulator.
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TemplateScreenPreview() {
    LoopyTheme { TemplateScreen("Loopy · Template", "Isi layar ini sesuai node Figma dan PRD.", {}) }
}
