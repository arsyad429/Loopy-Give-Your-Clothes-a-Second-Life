package id.ac.loopy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import id.ac.loopy.domain.model.DonationStatus

@Composable
internal fun StatusBadge(status: DonationStatus) {
    val colors = MaterialTheme.colorScheme
    val light = colors.background.luminance() > 0.5f
    val (label, background, foreground) = when (status) {
        DonationStatus.PENDING_REVIEW -> Triple("Menunggu verifikasi", colors.tertiaryContainer, colors.onTertiaryContainer)
        DonationStatus.PICKUP_SCHEDULED -> Triple("Dijadwalkan", if (light) Color(0xFFEEE7FA) else colors.secondaryContainer, if (light) Color(0xFF223D31) else colors.onSecondaryContainer)
        DonationStatus.PICKED_UP -> Triple("Dijemput", colors.primaryContainer, colors.onPrimaryContainer)
        DonationStatus.COMPLETED -> Triple("Selesai", if (light) Color(0xFFCFF2DF) else colors.primaryContainer, if (light) Color(0xFF246348) else colors.onPrimaryContainer)
        DonationStatus.REJECTED -> Triple("Ditolak", colors.errorContainer, colors.onErrorContainer)
    }

    Text(
        text = label,
        modifier = Modifier.background(background, CircleShape).padding(horizontal = 12.dp, vertical = 5.dp),
        color = foreground,
        style = MaterialTheme.typography.labelMedium,
    )
}
