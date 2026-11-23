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

    val trimmedReason = reason.trim()
    val reasonError = when {
        trimmedReason.isEmpty() -> "Tulis alasan penolakan sebelum melanjutkan."
        trimmedReason.codePointCount(0, trimmedReason.length) > 500 ->
            "Alasan terlalu panjang. Gunakan maksimal 500 karakter."
        '\u0000' in trimmedReason -> "Periksa kembali teks alasan penolakan."
        else -> null
    }

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
                    error = reasonError.takeIf { submitted },
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LoopyButton("Konfirmasi penolakan", onClick = {
                    submitted = true

                    if (reasonError == null) {
                        onConfirm(trimmedReason)
                    }
                })

                LoopyButton("Batal", onClick = onDismiss, outlined = true)
            }
        },
    )
}
