package id.ac.loopy.feature.auth

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

private const val MinimumPasswordLength = 15
private const val MaximumPasswordLength = 128
private const val MaximumPasswordBytes = 512

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onRegister: (name: String, email: String, password: String) -> Unit,
    feedback: String? = null,
    isSubmitting: Boolean = false,
    fieldErrors: Map<String, String> = emptyMap(),
    onFieldEdited: (String) -> Unit = {},
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val trimmedName = name.trim()
    val nameError = when {
        trimmedName.isEmpty() -> "Masukkan nama lengkap."
        trimmedName.codePointCount(0, trimmedName.length) > 100 -> "Nama terlalu panjang. Gunakan maksimal 100 karakter."
        trimmedName.any { it.isISOControl() } -> "Tulis ulang nama lengkap dalam satu baris."
        else -> null
    }

    val emailError = emailValidation(email)
    val passwordLength = password.codePointCount(0, password.length)
    val passwordError = when {
        password.isEmpty() -> "Buat kata sandi untuk akunmu."
        passwordLength < MinimumPasswordLength ->
            "Kata sandi terlalu pendek. Gunakan minimal $MinimumPasswordLength karakter."
        passwordLength > MaximumPasswordLength || password.toByteArray().size > MaximumPasswordBytes ->
            "Kata sandi terlalu panjang. Coba gunakan kata sandi yang lebih pendek."
        else -> null
    }

    LoopyScreen(
        title = "Buat akun",
        onBack = onBack,
        footer = {
            feedback?.let { LoopyInfo("Belum bisa membuat akun", it, isError = true) }

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
            onValueChange = {
                name = it
                onFieldEdited("name")
            },
            placeholder = "Nama lengkapmu",
            error = nameError.takeIf { submitted } ?: fieldErrors["name"],
            enabled = !isSubmitting,
        )

        LoopyTextField(
            label = "Email",
            value = email,
            onValueChange = {
                email = it
                onFieldEdited("email")
            },
            placeholder = "nama@email.com",
            error = emailError.takeIf { submitted } ?: fieldErrors["email"],
            keyboardType = KeyboardType.Email,
            enabled = !isSubmitting,
        )

        LoopyTextField(
            label = "Kata sandi",
            value = password,
            onValueChange = {
                password = it
                onFieldEdited("password")
            },
            placeholder = "Buat kata sandi",
            error = passwordError.takeIf { submitted } ?: fieldErrors["password"],
            keyboardType = KeyboardType.Password,
            secret = true,
            hint = "Minimal $MinimumPasswordLength karakter.",
            enabled = !isSubmitting,
        )

        TextButton(onClick = onLogin, enabled = !isSubmitting) {
            Text("Sudah punya akun? Masuk")
        }

        LoopyInfo("Kamu mulai dari 0 poin", "Dapatkan 10 poin setiap pengajuan donasi selesai.")
    }
}
