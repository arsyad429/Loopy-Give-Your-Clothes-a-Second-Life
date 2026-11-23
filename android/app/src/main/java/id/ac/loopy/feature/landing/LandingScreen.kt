package id.ac.loopy.feature.landing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import id.ac.loopy.R
import id.ac.loopy.ui.components.LoopyButton
import id.ac.loopy.ui.components.LoopyScreen

@Composable
fun LandingScreen(onLogin: () -> Unit, onRegister: () -> Unit, onBack: (() -> Unit)? = null) {
    LoopyScreen(
        title = "loopy ↻",
        onBack = onBack,
        footer = {
            LoopyButton("Mulai sekarang", onClick = onRegister)
            LoopyButton("Masuk", onClick = onLogin, outlined = true)
        },
    ) {
        val colors = MaterialTheme.colorScheme
        val type = MaterialTheme.typography

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(colors.primaryContainer, RoundedCornerShape(24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clothing_shirt),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(72.dp),
            )

            Text("Baju lama,\ncerita baru.", color = colors.onPrimaryContainer, style = type.headlineMedium)
            Text(
                "Donasikan pakaian yang masih layak. Kami bantu cek dan atur penjemputannya.",
                color = colors.onPrimaryContainer,
                style = type.bodyMedium,
            )
        }

        Text("Kenapa Loopy?", style = type.titleLarge)

        LandingBenefit("Cek dengan AI", "Periksa kondisi awal pakaian dari foto.")
        LandingBenefit("Pickup lebih mudah", "Ajukan penjemputan dari rumah.")
        LandingBenefit("Kebaikan dapat apresiasi", "Dapatkan poin setelah donasi selesai.")

        LandingBenefit(
            "Empat langkah sederhana",
            "Cek pakaian, ajukan penjemputan, pantau donasi, lalu tukarkan poin.",
        )
    }
}

@Composable
private fun LandingBenefit(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
