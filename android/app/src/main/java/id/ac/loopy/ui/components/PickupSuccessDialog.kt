package id.ac.loopy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.feature.pickup.PickupRequestValidator
import id.ac.loopy.feature.pickup.SubmittedPickupSummary
import id.ac.loopy.ui.theme.LoopyTheme

@Composable
internal fun PickupSuccessDialog(
    summary: SubmittedPickupSummary,
    onGoToMyDonations: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    Dialog(onDismissRequest = { onDismiss?.invoke() ?: onGoToMyDonations() }) {
        LoopyCard(padding = 24, spacing = 16) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                LoopyAsset("check", size = 28, tint = MaterialTheme.colorScheme.primary)
            }

            Column(
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "Pengajuan terkirim!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "ID pengajuan #${summary.shortId} berhasil disimpan dan menunggu peninjauan admin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusBadge(summary.status)
                Text(
                    "${summary.quantity} pakaian · ${summary.city}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            LoopyCard(
                color = MaterialTheme.colorScheme.primaryContainer,
                padding = 14,
                spacing = 4,
                radius = 16,
            ) {
                Text(
                    "Preferensi penjemputan",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "${PickupRequestValidator.formatReadableDate(summary.preferredDate)} · ${summary.preferredTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Jadwal final akan dikonfirmasi oleh admin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            LoopyButton(
                label = "Lihat My Donations",
                onClick = onGoToMyDonations,
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(fontScale = 1.3f, showBackground = true)
@Composable
private fun PickupSuccessDialogPreview() {
    LoopyTheme {
        Box(Modifier.padding(16.dp)) {
            PickupSuccessDialog(
                summary = SubmittedPickupSummary(
                    donationId = "LP128",
                    status = DonationStatus.PENDING_REVIEW,
                    quantity = 3,
                    city = "Malang",
                    preferredDate = "2026-11-28",
                    preferredTime = "Pagi (08.00–11.00)",
                ),
                onGoToMyDonations = {},
            )
        }
    }
}
