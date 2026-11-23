package id.ac.loopy.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.loopy.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginViewModel(private val auth: AuthRepositoryImpl) : ViewModel() {
    val session = auth.session

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback = _feedback.asStateFlow()

    fun login(email: String, password: String) = submit {
        auth.login(email, password)
    }

    fun register(name: String, email: String, password: String) = submit {
        auth.register(name, email, password)
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    private fun submit(request: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        _feedback.value = null

        viewModelScope.launch {
            try {
                request()
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                _feedback.value = when (e.code()) {
                    401 -> "Email atau kata sandi salah."
                    409 -> "Email sudah terdaftar."
                    422 -> "Periksa kembali data yang dimasukkan."
                    429 -> "Terlalu banyak percobaan. Coba lagi nanti."
                    else -> "Permintaan gagal. Coba lagi."
                }
            } catch (_: IOException) {
                _feedback.value = "Backend Go tidak dapat dihubungi."
            } finally {
                _busy.value = false
            }
        }
    }
}
