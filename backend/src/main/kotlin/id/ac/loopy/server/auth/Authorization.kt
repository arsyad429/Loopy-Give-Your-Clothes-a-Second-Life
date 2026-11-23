package id.ac.loopy.server.auth

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import java.util.UUID

const val LOOPY_AUTH = "loopy-bearer"

enum class UserRole { USER, ADMIN }

data class LoopyPrincipal(val userId: UUID, val role: UserRole)

fun Application.installLoopyAuthentication(sessions: SessionStore) {
    install(Authentication) {
        bearer(LOOPY_AUTH) {
            realm = "Loopy"
            authenticate { credentials -> sessions.verify(credentials.token) }
        }
    }
}
