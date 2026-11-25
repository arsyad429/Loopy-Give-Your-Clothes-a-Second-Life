package id.ac.loopy.feature.settings

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.loopy.data.remote.dto.ProfileUpdateRequest
import id.ac.loopy.feature.auth.MaximumPasswordBytes
import id.ac.loopy.feature.auth.MaximumPasswordLength
import id.ac.loopy.feature.auth.MinimumPasswordLength
import id.ac.loopy.feature.auth.usernameValidation
import id.ac.loopy.feature.profile.AccountViewModel
import id.ac.loopy.ui.components.LoopyButton
import id.ac.loopy.ui.components.LoopyCard
import id.ac.loopy.ui.components.LoopyInfo
import id.ac.loopy.ui.components.LoopyIntro
import id.ac.loopy.ui.components.LoopyScreen
import id.ac.loopy.ui.components.LoopyTextField

@Composable
internal fun SettingsScreen(model: AccountViewModel, onBack: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(model) { model.refresh() }

    LoopyScreen("Settings", onBack) {
        val profile = state.profile
        if (profile == null) {
            if (state.loading) CircularProgressIndicator()
            state.error?.let {
                LoopyInfo("Pengaturan belum tersedia", it, isError = true)
                LoopyButton("Muat ulang", model::refresh)
            }
            return@LoopyScreen
        }

        var name by rememberSaveable(profile.userId, profile.name) { mutableStateOf(profile.name) }
        var username by rememberSaveable(profile.userId, profile.username) { mutableStateOf(profile.username.orEmpty()) }
        var current by remember(profile.userId) { mutableStateOf("") }
        var password by remember(profile.userId) { mutableStateOf("") }
        var confirmation by remember(profile.userId) { mutableStateOf("") }
        var submitted by remember { mutableStateOf(false) }

        LaunchedEffect(state.saved) {
            if (state.saved) {
                current = ""
                password = ""
                confirmation = ""
                submitted = false
            }
        }

        val editingPassword = current.isNotEmpty() || password.isNotEmpty() || confirmation.isNotEmpty()
        val trimmedName = name.trim()
        val nameError = if (trimmedName.isEmpty() || trimmedName.codePointCount(0, trimmedName.length) > 100 ||
            trimmedName.any { it.isISOControl() }
        ) "Masukkan 1–100 karakter dalam satu baris." else null
        val usernameError = usernameValidation(username)
        val currentError = if (editingPassword && current.isEmpty()) "Masukkan kata sandi saat ini." else null
        val passwordError = if (editingPassword && (password.codePointCount(0, password.length) !in MinimumPasswordLength..MaximumPasswordLength ||
                password.toByteArray().size > MaximumPasswordBytes)
        ) "Gunakan 8–128 karakter." else null
        val confirmationError = if (editingPassword && confirmation != password) "Konfirmasi harus sama dengan kata sandi baru." else null
        val enabled = !state.saving

        LoopyIntro("Pengaturan akun", "Ubah nama, username, atau kata sandi.")
        if (state.saved) LoopyInfo("Perubahan tersimpan", "Data akun berhasil diperbarui.")
        state.error?.let { LoopyInfo("Perubahan belum tersimpan", it, isError = true) }

        LoopyCard {
            LoopyTextField(
                label = "Nama lengkap",
                value = name,
                onValueChange = {
                    name = it
                    model.clearFeedback("name")
                },
                error = nameError.takeIf { submitted } ?: state.fieldErrors["name"], enabled = enabled,
            )
            LoopyTextField(
                label = "Username",
                value = username,
                onValueChange = {
                    username = it
                    model.clearFeedback("username")
                },
                error = usernameError.takeIf { submitted } ?: state.fieldErrors["username"], enabled = enabled,
            )
            LoopyInfo(if (profile.emailVerified) "Email terverifikasi" else "Email akun", profile.email)
        }

        LoopyCard {
            Text("Ganti kata sandi", style = MaterialTheme.typography.titleLarge)
            Text("Kosongkan bagian ini jika hanya mengubah nama atau username.", style = MaterialTheme.typography.bodySmall)
            LoopyTextField(
                label = "Kata sandi saat ini",
                value = current,
                onValueChange = {
                    current = it
                    model.clearFeedback("currentPassword")
                },
                error = currentError.takeIf { submitted } ?: state.fieldErrors["currentPassword"],
                keyboardType = KeyboardType.Password, secret = true, enabled = enabled,
            )
            LoopyTextField(
                label = "Kata sandi baru",
                value = password,
                onValueChange = {
                    password = it
                    model.clearFeedback("newPassword")
                },
                error = passwordError.takeIf { submitted } ?: state.fieldErrors["newPassword"],
                keyboardType = KeyboardType.Password, secret = true, enabled = enabled,
            )
            LoopyTextField(
                label = "Konfirmasi kata sandi baru",
                value = confirmation,
                onValueChange = {
                    confirmation = it
                    model.clearFeedback("confirmation")
                },
                error = confirmationError.takeIf { submitted }, keyboardType = KeyboardType.Password, secret = true, enabled = enabled,
            )
        }

        LoopyButton(if (state.saving) "Menyimpan…" else "Simpan perubahan", enabled = enabled, onClick = {
            submitted = true
            if (nameError == null && usernameError == null && currentError == null && passwordError == null && confirmationError == null) {
                model.save(
                    ProfileUpdateRequest(
                        name = trimmedName,
                        username = username.trim(),
                        currentPassword = current.takeIf { editingPassword },
                        newPassword = password.takeIf { editingPassword },
                    ),
                )
            }
        })
    }
}
