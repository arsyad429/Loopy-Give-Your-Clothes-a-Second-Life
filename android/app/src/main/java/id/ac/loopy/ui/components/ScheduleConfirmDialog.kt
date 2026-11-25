package id.ac.loopy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ScheduleConfirmDialog(
    summary: String,
    schedule: String,
    address: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfirmasi jadwal") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(summary)
                Text(schedule, style = MaterialTheme.typography.titleMedium)
                Text(address)
                Text("Pengajuan akan berubah menjadi Dijadwalkan.")
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LoopyButton("Setujui & simpan jadwal", onClick = onConfirm)
                LoopyButton("Batal", onClick = onDismiss, outlined = true)
            }
        },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    )
}
