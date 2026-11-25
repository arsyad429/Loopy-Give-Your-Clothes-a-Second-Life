package id.ac.loopy.feature.auth

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import id.ac.loopy.data.remote.dto.RegistrationChallengeDto
import id.ac.loopy.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

internal data class AuthFeedback(
    val message: String? = null,
    val fields: Map<String, String> = emptyMap(),
)

internal data class EmailRegistrationState(
    val challenge: RegistrationChallengeDto,
    val receivedAt: Long = SystemClock.elapsedRealtime(),
    val error: String? = null,
    val codeLocked: Boolean = false,
)

private const val ServiceUnavailableMessage = "Layanan sedang bermasalah. Coba lagi sebentar."

private val RegistrationFieldMessages = mapOf(
    "name" to "Periksa nama lengkap. Gunakan maksimal 100 karakter.",
    "email" to "Periksa alamat email. Gunakan format seperti nama@email.com.",
    "username" to "Gunakan 3–20 huruf, angka, atau underscore.",
    "password" to "Coba gunakan frasa dari beberapa kata sebagai kata sandi.",
)

class LoginViewModel(private val auth: AuthRepositoryImpl) : ViewModel() {
    val session = auth.session

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    private val _loginFeedback = MutableStateFlow<AuthFeedback?>(null)
    internal val loginFeedback = _loginFeedback.asStateFlow()

    private val _registerFeedback = MutableStateFlow<AuthFeedback?>(null)
    internal val registerFeedback = _registerFeedback.asStateFlow()

    fun login(email: String, password: String) = submit(_loginFeedback, registration = false) {
        auth.login(email, password)
    }

    private val _emailRegistration = MutableStateFlow<EmailRegistrationState?>(null)
    internal val emailRegistration = _emailRegistration.asStateFlow()

    fun register(name: String, email: String, password: String, username: String) = submit(_registerFeedback, registration = true) {
        _emailRegistration.value = auth.register(name, email, password, username)?.let { EmailRegistrationState(it) }
    }

    fun cancelEmailRegistration() {
        if (!_busy.value) _emailRegistration.value = null
    }

    fun clearCodeError() {
        val active = _emailRegistration.value ?: return
        if (!active.codeLocked) _emailRegistration.value = active.copy(error = null)
    }

    fun verifyRegistration(code: String) = submitCode { active ->
        auth.verifyRegistration(active.challenge.challengeId, code)
        _emailRegistration.value = null
    }

    fun resendRegistration() = submitCode { active ->
        val challenge = auth.resendRegistration(active.challenge.challengeId)
        _emailRegistration.value = EmailRegistrationState(challenge)
    }

    private fun submitCode(action: suspend (EmailRegistrationState) -> Unit) {
        val active = _emailRegistration.value ?: return
        if (_busy.value) return
        _busy.value = true
        _emailRegistration.value = active.copy(error = null)
        viewModelScope.launch {
            try {
                action(active)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: HttpException) {
                val code = error.details()?.get("code")?.takeIf { it.isJsonPrimitive }?.asString
                val message = when (code) {
                    "OTP_INVALID" -> "Kode belum tepat. Periksa email lalu coba lagi."
                    "OTP_EXPIRED" -> "Kode sudah kedaluwarsa. Minta kode baru atau isi ulang formulir."
                    "OTP_LOCKED" -> "Percobaan kode habis. Minta kode baru."
                    "OTP_COOLDOWN", "RATE_LIMITED" -> "Tunggu sebentar sebelum meminta atau memasukkan kode lagi."
                    "EMAIL_ALREADY_REGISTERED" -> "Email ini sudah terdaftar. Kembali lalu masuk."
                    "USERNAME_ALREADY_REGISTERED" -> "Username sudah dipakai. Kembali lalu pilih username lain."
                    else -> "Verifikasi belum berhasil. Coba lagi."
                }
                _emailRegistration.value = active.copy(
                    error = message,
                    codeLocked = active.codeLocked || code == "OTP_LOCKED" || code == "OTP_EXPIRED",
                )
            } catch (_: Exception) {
                _emailRegistration.value = active.copy(error = "Periksa koneksi lalu coba lagi.")
            } finally {
                _busy.value = false
            }
        }
    }

    private val _logoutError = MutableStateFlow<String?>(null)
    val logoutError = _logoutError.asStateFlow()

    fun logout(onSuccess: () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        _logoutError.value = null
        viewModelScope.launch {
            try {
                auth.logout()
                clearFeedback()
                onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _logoutError.value = "Sesi belum bisa diakhiri. Periksa koneksi, lalu coba lagi."
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearFeedback() {
        _loginFeedback.value = null
        _registerFeedback.value = null
    }

    fun clearRegisterFieldFeedback(field: String) {
        val feedback = _registerFeedback.value ?: return
        val remaining = feedback.fields - field

        _registerFeedback.value = AuthFeedback(fields = remaining).takeIf { remaining.isNotEmpty() }
    }

    private fun submit(
        feedback: MutableStateFlow<AuthFeedback?>,
        registration: Boolean,
        request: suspend () -> Unit,
    ) {
        if (_busy.value) return
        _busy.value = true
        feedback.value = null

        viewModelScope.launch {
            try {
                request()
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                feedback.value = authHttpFeedback(e, registration)
            } catch (_: SocketTimeoutException) {
                feedback.value = AuthFeedback("Koneksi terlalu lama. Coba lagi sebentar.")
            } catch (_: JsonParseException) {
                feedback.value = AuthFeedback(ServiceUnavailableMessage)
            } catch (_: IOException) {
                feedback.value = AuthFeedback("Tidak bisa terhubung. Periksa koneksimu, lalu coba lagi.")
            } finally {
                _busy.value = false
            }
        }
    }
}

internal fun authHttpFeedback(error: HttpException, registration: Boolean): AuthFeedback {
    return when {
        !registration && error.code() == 401 ->
            AuthFeedback("Email atau kata sandi salah. Periksa kembali, lalu coba lagi.")

        registration && error.code() == 409 -> {
            val code = error.details()?.get("code")?.takeIf { it.isJsonPrimitive }?.asString
            if (code == "USERNAME_ALREADY_REGISTERED") AuthFeedback(fields = mapOf("username" to "Username sudah dipakai."))
            else AuthFeedback(fields = mapOf("email" to "Email ini sudah terdaftar. Masuk atau gunakan email lain."))
        }

        registration && error.code() == 503 ->
            AuthFeedback("Email verifikasi belum bisa dikirim. Coba lagi nanti.")

        registration && error.code() == 422 -> {
            val fieldNames = error.details()?.get("fieldErrors")
                ?.takeIf { it.isJsonObject }?.asJsonObject?.keySet().orEmpty()
            val fields = RegistrationFieldMessages.filterKeys { it in fieldNames }

            if (fields.isEmpty()) AuthFeedback("Periksa nama, email, dan kata sandi, lalu coba lagi.")
            else AuthFeedback(fields = fields)
        }

        error.code() == 429 -> {
            val code = error.details()?.get("code")
                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
            val message = when (code) {
                "OTP_COOLDOWN" -> "Kode untuk email ini masih aktif. Coba daftar lagi setelah kode kedaluwarsa."
                "RATE_LIMITED" -> "Terlalu banyak percobaan dalam waktu singkat. Tunggu sebentar, lalu coba lagi."
                else -> "Layanan sedang sibuk. Tunggu sebentar, lalu coba lagi."
            }
            AuthFeedback(message)
        }

        else -> AuthFeedback(ServiceUnavailableMessage)
    }
}

internal fun HttpException.details(): JsonObject? {
    return try {
        response()?.errorBody()?.use { body ->
            JsonParser.parseReader(body.charStream()).takeIf { it.isJsonObject }?.asJsonObject
                ?.get("error")?.takeIf { it.isJsonObject }?.asJsonObject
        }
    } catch (_: JsonParseException) {
        null
    } catch (_: IOException) {
        null
    }
}
