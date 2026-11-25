package id.ac.loopy.feature.verification

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.ac.loopy.ui.components.LoopyButton
import id.ac.loopy.feature.auth.EmailRegistrationState
import id.ac.loopy.ui.components.LoopyInfo
import id.ac.loopy.ui.components.LoopyIntro
import id.ac.loopy.ui.components.LoopyScreen
import kotlinx.coroutines.delay

@Composable
internal fun EmailVerificationScreen(
    state: EmailRegistrationState,
    busy: Boolean,
    onBack: () -> Unit,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onCodeEdit: () -> Unit,
) {
    var code by remember(state.receivedAt) { mutableStateOf("") }
    var now by remember(state.receivedAt) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val lifetimeMs = state.challenge.expiresInSeconds * 1_000L

    LaunchedEffect(state.receivedAt) {
        while (now - state.receivedAt < lifetimeMs) {
            delay(1_000)
            now = SystemClock.elapsedRealtime()
        }
    }

    val focusManager = LocalFocusManager.current
    val elapsed = (now - state.receivedAt).coerceAtLeast(0)
    val expired = elapsed >= lifetimeMs
    val secondsToResend = ((state.challenge.resendAfterSeconds * 1_000L - elapsed + 999) / 1_000).coerceAtLeast(0)
    BackHandler { if (!busy) onBack() }

    LoopyScreen(
        title = "Verifikasi email",
        onBack = { if (!busy) onBack() },
        footer = {
            LoopyButton(
                label = if (busy) "Memproses…" else "Verifikasi & lanjut",
                enabled = code.length == 6 && !expired && !busy && !state.codeLocked,
                onClick = {
                    focusManager.clearFocus()
                    onVerify(code)
                },
            )
        },
    ) {
        LoopyIntro("Masukkan kode", "Kami mengirim kode 6 digit ke ${state.challenge.maskedEmail}.")

        VerificationCodeField(code, isError = state.error != null || expired) {
            code = it
            onCodeEdit()
        }

        when {
            state.error != null -> LoopyInfo("Verifikasi email", state.error, isError = true)
            expired -> LoopyInfo("Kode kedaluwarsa", "Minta kode baru untuk melanjutkan.", isError = true)
            else -> Text("Kode berlaku 5 menit. Periksa juga folder spam.", style = MaterialTheme.typography.bodyMedium)
        }

        TextButton(enabled = !busy && secondsToResend == 0L, onClick = onResend) {
            Text(if (secondsToResend > 0) "Kirim ulang kode dalam $secondsToResend detik" else "Kirim ulang kode")
        }
        TextButton(enabled = !busy, onClick = onBack) {
            Text("Email salah? Ubah email")
        }
    }
}

@Composable
private fun VerificationCodeField(code: String, isError: Boolean, onCodeChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    BasicTextField(
        value = code,
        onValueChange = { onCodeChange(it.filter { digit -> digit in '0'..'9' }.take(6)) },
        modifier = Modifier.fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = "Kode verifikasi 6 digit" },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        decorationBox = { input ->
            Box {
                Box(Modifier.matchParentSize().alpha(0f)) {
                    input()
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(6) { index ->
                        val border = when {
                            isError -> colors.error
                            focused && index == code.length -> colors.primary
                            else -> colors.outlineVariant
                        }

                        val shape = RoundedCornerShape(12.dp)

                        Box(
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                                .background(colors.surface, shape).border(1.dp, border, shape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(code.getOrNull(index)?.toString() ?: "–", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        },
    )
}
