package id.ac.loopy.feature.verification

import android.os.SystemClock
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import id.ac.loopy.ui.components.LoopyInfo
import id.ac.loopy.ui.components.LoopyIntro
import id.ac.loopy.ui.components.LoopyScreen
import kotlinx.coroutines.delay

private const val DemoCode = "123456"
private const val CodeLifetime = 300_000L
private const val ResendDelay = 60_000L

@Composable
fun EmailVerificationScreen(
    maskedEmail: String,
    onBack: () -> Unit,
    onEditEmail: () -> Unit,
    onVerified: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var incorrectCode by remember { mutableStateOf(false) }
    var sentAt by rememberSaveable { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(sentAt) {
        while (now - sentAt < CodeLifetime) {
            now = SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }

    val focusManager = LocalFocusManager.current
    val elapsed = (now - sentAt).coerceAtLeast(0)
    val expired = elapsed >= CodeLifetime
    val secondsToResend = ((ResendDelay - elapsed + 999) / 1_000).coerceAtLeast(0)

    LoopyScreen(
        title = "Verifikasi email",
        onBack = onBack,
        footer = {
            LoopyButton(
                label = "Verifikasi & lanjut",
                enabled = code.length == 6 && !expired,
                onClick = {
                    val submittedAt = SystemClock.elapsedRealtime()
                    now = submittedAt

                    if (submittedAt - sentAt < CodeLifetime) {
                        if (code == DemoCode) {
                            focusManager.clearFocus()
                            onVerified()
                        } else {
                            incorrectCode = true
                        }
                    }
                },
            )
        },
    ) {
        LoopyIntro("Cek emailmu", "Masukkan 6 digit kode untuk $maskedEmail.")

        VerificationCodeField(code, isError = incorrectCode || expired) {
            code = it
            incorrectCode = false
        }

        when {
            expired -> LoopyInfo("Kode kedaluwarsa", "Minta kode baru untuk melanjutkan.", isError = true)
            incorrectCode -> LoopyInfo("Kode tidak cocok", "Periksa kembali keenam digit kode.", isError = true)
            else -> LoopyInfo("Kode berlaku 5 menit", "Pada pratinjau, gunakan kode $DemoCode.")
        }

        TextButton(
            enabled = secondsToResend == 0L,
            onClick = {
                now = SystemClock.elapsedRealtime()
                sentAt = now
                code = ""
                incorrectCode = false
            },
        ) {
            Text(if (secondsToResend > 0) "Kirim ulang kode dalam $secondsToResend detik" else "Kirim ulang kode")
        }

        TextButton(onClick = onEditEmail) {
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
