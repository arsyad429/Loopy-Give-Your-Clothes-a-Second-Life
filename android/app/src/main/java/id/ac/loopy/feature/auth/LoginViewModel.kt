package id.ac.loopy.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
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

private const val ServiceUnavailableMessage = "Layanan sedang bermasalah. Coba lagi sebentar."

private val RegistrationFieldMessages = mapOf(
    "name" to "Periksa nama lengkap. Gunakan maksimal 100 karakter.",
    "email" to "Periksa alamat email. Gunakan format seperti nama@email.com.",
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

    fun register(name: String, email: String, password: String) = submit(_registerFeedback, registration = true) {
        auth.register(name, email, password)
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

        registration && error.code() == 409 ->
            AuthFeedback(fields = mapOf("email" to "Email ini sudah terdaftar. Masuk atau gunakan email lain."))

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
            val message = if (code == "RATE_LIMITED") {
                "Terlalu banyak percobaan dalam waktu singkat. Tunggu sebentar, lalu coba lagi."
            } else {
                "Layanan sedang sibuk. Tunggu sebentar, lalu coba lagi."
            }
            AuthFeedback(message)
        }

        else -> AuthFeedback(ServiceUnavailableMessage)
    }
}

private fun HttpException.details(): JsonObject? {
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
