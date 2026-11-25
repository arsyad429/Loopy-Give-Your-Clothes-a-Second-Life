package id.ac.loopy.data.repository

import id.ac.loopy.data.remote.api.AuthApi
import id.ac.loopy.data.remote.dto.ProfileUpdateRequest
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AuthRepositoryHttpTest {
    @Test fun registrationKeepsSessionEmptyUntilEmailCodeIsVerified() = runBlocking {
        MockWebServer().use { server ->
            val challenge = """{"challengeId":"challenge-1","maskedEmail":"d***@example.test","expiresInSeconds":300,"resendAfterSeconds":60}"""
            val user = """{"userId":"donor-1","name":"Dara","role":"USER","email":"dara@example.test","username":"dara_user","emailVerified":true}"""
            val session = """{"accessToken":"token-1","refreshToken":"refresh-1","user":$user}"""
            val updated = user.replace("Dara", "Dara Baru")
            server.enqueue(response(challenge, 202))
            server.enqueue(response(challenge, 202))
            server.enqueue(response(session, 201))
            server.enqueue(response(updated, 200))
            server.start()
            val auth = repository(server)

            val pending = auth.register("Dara", "dara@example.test", "Sample!8", "dara_user")
            assertNotNull(pending)
            assertEquals("d***@example.test", pending?.maskedEmail)
            assertNull(auth.session.value)
            assertEquals(pending, auth.resendRegistration("challenge-1"))
            assertNull(auth.session.value)

            auth.verifyRegistration("challenge-1", "234567")
            assertEquals(true, auth.session.value?.user?.emailVerified)
            auth.updateProfile(ProfileUpdateRequest("Dara Baru", "dara_user"))
            assertEquals("Dara Baru", auth.session.value?.user?.name)

            val requests = List(4) { requireNotNull(server.takeRequest(1, TimeUnit.SECONDS)) }
            assertEquals(listOf("/v1/auth/register", "/v1/auth/register/resend", "/v1/auth/register/verify", "/v1/auth/me"), requests.map { it.path })
            assertEquals("""{"challengeId":"challenge-1","code":"234567"}""", requests[2].body.readUtf8())
            assertEquals("PATCH", requests[3].method)
            assertEquals("Bearer token-1", requests[3].getHeader("Authorization"))
        }
    }

    @Test fun localRegistrationWithoutEmailVerificationStillReadsItsSession() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(response("""{"accessToken":"token-1","refreshToken":"refresh-1","user":{"userId":"donor-1","name":"Dara","role":"USER"}}""", 201))
            server.start()
            val auth = repository(server)
            assertNull(auth.register("Dara", "dara@example.test", "Sample!8", ""))
            assertEquals("token-1", auth.session.value?.accessToken)
        }
    }

    private fun response(body: String, status: Int) =
        MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body)

    private fun repository(server: MockWebServer): AuthRepositoryImpl = AuthRepositoryImpl(
        Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(AuthApi::class.java)
    )
}
