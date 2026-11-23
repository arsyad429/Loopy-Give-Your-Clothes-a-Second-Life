package id.ac.loopy.data.repository

import id.ac.loopy.data.remote.api.AuthApi
import id.ac.loopy.data.remote.dto.LoginRequest
import id.ac.loopy.data.remote.dto.RefreshRequest
import id.ac.loopy.data.remote.dto.RegisterRequest
import id.ac.loopy.data.remote.dto.SessionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

class AuthRepositoryImpl(private val api: AuthApi) {
    private val mutex = Mutex()
    private val current = MutableStateFlow<SessionDto?>(null)
    val session = current.asStateFlow()

    suspend fun login(email: String, password: String) = mutex.withLock {
        current.value = api.login(LoginRequest(email, password))
    }

    suspend fun register(name: String, email: String, password: String) = mutex.withLock {
        current.value = api.register(RegisterRequest(name, email, password))
    }

    suspend fun <T> withBearer(request: suspend (String) -> T): T {
        val original = current.value ?: error("Silakan masuk terlebih dahulu.")
        try {
            return request("Bearer ${original.accessToken}")
        } catch (e: HttpException) {
            if (e.code() != 401) throw e
        }

        val renewed = mutex.withLock {
            val active = current.value ?: error("Sesi berakhir. Silakan masuk kembali.")
            check(active.user.userId == original.user.userId) { "Akun telah berubah." }

            if (active.accessToken != original.accessToken) {
                active
            } else {
                try {
                    api.refresh(RefreshRequest(active.refreshToken)).also {
                        current.value = it
                    }
                } catch (e: HttpException) {
                    if (e.code() == 401) current.value = null
                    throw e
                }
            }
        }

        return request("Bearer ${renewed.accessToken}")
    }
}
