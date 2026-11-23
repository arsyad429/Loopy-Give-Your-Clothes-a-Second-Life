package id.ac.loopy.feature.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.ac.loopy.R
import id.ac.loopy.ui.components.LoopyButton
import id.ac.loopy.ui.components.LoopyInfo
import id.ac.loopy.ui.components.LoopyIntro
import id.ac.loopy.ui.components.LoopyScreen
import id.ac.loopy.ui.components.LoopyTextField

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onRegister: () -> Unit,
    onLogin: (email: String, password: String) -> Unit,
    feedback: String? = null,
    isSubmitting: Boolean = false,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val emailError = emailValidation(email)
    val passwordError = if (password.isEmpty()) "Masukkan kata sandi." else null

    LoopyScreen(
        title = "loopy ↻",
        onBack = onBack,
        footer = {
            LoopyButton(
                label = if (isSubmitting) "Sedang masuk…" else "Masuk",
                enabled = !isSubmitting,
                onClick = {
                    submitted = true
                    if (emailError == null && passwordError == null) {
                        focusManager.clearFocus()
                        onLogin(email.trim(), password)
                    }
                },
            )
        },
    ) {
        val colors = MaterialTheme.colorScheme

        Row(
            modifier = Modifier.fillMaxWidth().background(colors.primaryContainer, RoundedCornerShape(20.dp)).padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clothing_shirt),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(52.dp),
            )

            Text("Selamat datang kembali", style = MaterialTheme.typography.titleMedium, color = colors.onPrimaryContainer)
        }

        LoopyIntro("Senang kamu\nkembali!", "Masuk untuk melanjutkan langkah baikmu.")

        LoopyTextField(
            label = "Email",
            value = email,
            onValueChange = { email = it },
            placeholder = "nama@email.com",
            error = emailError.takeIf { submitted },
            keyboardType = KeyboardType.Email,
        )

        LoopyTextField(
            label = "Kata sandi",
            value = password,
            onValueChange = { password = it },
            placeholder = "Masukkan kata sandi",
            error = passwordError.takeIf { submitted },
            keyboardType = KeyboardType.Password,
            secret = true,
        )

        if (feedback != null) {
            LoopyInfo("Status masuk", feedback)
        }

        TextButton(onClick = onRegister, enabled = !isSubmitting) {
            Text("Belum punya akun? Daftar")
        }

        LoopyInfo("Satu akun, banyak kebaikan", "Pengguna dan admin masuk dari halaman ini.")
    }
}

internal fun emailValidation(value: String): String? {
    val email = value.trim()

    return when {
        email.isEmpty() -> "Masukkan alamat email."
        email.toByteArray().size > 254 || !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
            "Masukkan alamat email yang valid."
        else -> null
    }
}
