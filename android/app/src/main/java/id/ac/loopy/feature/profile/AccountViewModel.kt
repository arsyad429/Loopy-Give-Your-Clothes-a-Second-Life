package id.ac.loopy.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.loopy.data.remote.dto.AuthUserDto
import id.ac.loopy.data.remote.dto.ProfileUpdateRequest
import id.ac.loopy.data.repository.AuthRepositoryImpl
import id.ac.loopy.feature.auth.details
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

internal data class AccountUiState(
    val profile: AuthUserDto? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
)

internal class AccountViewModel(private val auth: AuthRepositoryImpl) : ViewModel() {
    private val _state = MutableStateFlow(AccountUiState())
    val state = _state.asStateFlow()

    fun refresh() {
        if (_state.value.loading || _state.value.saving) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val profile = auth.profile()
                _state.update { it.copy(profile = profile) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(error = "Profil gagal dimuat. Coba lagi.") }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun save(request: ProfileUpdateRequest) {
        if (_state.value.saving || _state.value.loading) return
        _state.update { it.copy(saving = true, saved = false, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            try {
                _state.value = AccountUiState(profile = auth.updateProfile(request), saved = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: HttpException) {
                val details = error.details()
                val fieldNames = details?.get("fieldErrors")?.takeIf { it.isJsonObject }?.asJsonObject?.keySet().orEmpty()
                val messages = mapOf(
                    "name" to "Masukkan nama yang valid.",
                    "username" to if (error.code() == 409) "Username sudah dipakai." else "Gunakan 3–20 huruf, angka, atau underscore.",
                    "currentPassword" to "Periksa kata sandi saat ini.",
                    "newPassword" to "Gunakan 8–128 karakter untuk kata sandi baru.",
                ).filterKeys { it in fieldNames }
                _state.update {
                    it.copy(
                        saving = false,
                        error = if (messages.isEmpty()) "Perubahan gagal disimpan. Muat ulang profil lalu coba lagi." else null,
                        fieldErrors = messages,
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(saving = false, error = "Perubahan gagal disimpan. Periksa koneksi lalu coba lagi.") }
            }
        }
    }

    fun clearFeedback(field: String) {
        _state.update { it.copy(saved = false, error = null, fieldErrors = it.fieldErrors - field) }
    }
}
