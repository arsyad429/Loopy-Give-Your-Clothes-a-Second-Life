package id.ac.loopy.feature.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import id.ac.loopy.ui.components.LoopyButton
import id.ac.loopy.ui.components.LoopyInfo
import id.ac.loopy.ui.components.LoopyIntro
import id.ac.loopy.ui.components.LoopyScreen
import id.ac.loopy.ui.components.LoopyTextField

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onRegister: (name: String, email: String, password: String) -> Unit,
    feedback: String? = null,
    isSubmitting: Boolean = false,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val trimmedName = name.trim()
    val nameError = when {
        trimmedName.isEmpty() -> "Masukkan nama lengkap."
        trimmedName.codePointCount(0, trimmedName.length) > 100 -> "Nama maksimal 100 karakter."
        trimmedName.any { it.isISOControl() } -> "Periksa karakter pada nama lengkap."
        else -> null
    }

    val emailError = emailValidation(email)
    val passwordError = when {
        password.isEmpty() -> "Masukkan kata sandi."
        password.codePointCount(0, password.length) !in 15..128 || password.toByteArray().size > 512 ->
            "Gunakan 15–128 karakter dengan ukuran maksimal 512 byte."
        else -> null
    }

    LoopyScreen(
        title = "Buat akun",
        onBack = onBack,
        footer = {
            LoopyButton(
                label = if (isSubmitting) "Membuat akun…" else "Buat akun",
                enabled = !isSubmitting,
                onClick = {
                    submitted = true
                    if (nameError == null && emailError == null && passwordError == null) {
                        focusManager.clearFocus()
                        onRegister(trimmedName, email.trim(), password)
                    }
                },
            )
        },
    ) {
        LoopyIntro("Mulai langkah\nbaikmu.", "Pakaian yang jarang dipakai bisa berarti bagi orang lain.")

        LoopyTextField(
            label = "Nama lengkap",
            value = name,
            onValueChange = { name = it },
            placeholder = "Nama lengkapmu",
            error = nameError.takeIf { submitted },
        )

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
            placeholder = "Buat kata sandi",
            error = passwordError.takeIf { submitted },
            keyboardType = KeyboardType.Password,
            secret = true,
        )

        Text("Gunakan 15–128 karakter.", style = MaterialTheme.typography.bodySmall)

        if (feedback != null) {
            LoopyInfo("Status pendaftaran", feedback)
        }

        TextButton(onClick = onLogin, enabled = !isSubmitting) {
            Text("Sudah punya akun? Masuk")
        }

        LoopyInfo("Kamu mulai dari 0 poin", "Dapatkan 10 poin setiap pengajuan donasi selesai.")
    }
}
