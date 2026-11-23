package id.ac.loopy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun RejectRequestDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tolak pengajuan?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Jelaskan alasan agar donatur memahami keputusanmu.")

                LoopyTextField(
                    label = "Alasan penolakan",
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = "Tulis alasan penolakan",
                    error = "Alasan penolakan wajib diisi.".takeIf { submitted && reason.isBlank() },
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LoopyButton("Konfirmasi penolakan", onClick = {
                    submitted = true

                    if (reason.isNotBlank()) {
                        onConfirm(reason.trim())
                    }
                })

                LoopyButton("Batal", onClick = onDismiss, outlined = true)
            }
        },
    )
}
