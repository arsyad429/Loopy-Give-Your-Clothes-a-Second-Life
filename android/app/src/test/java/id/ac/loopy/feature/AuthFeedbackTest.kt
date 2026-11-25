package id.ac.loopy.feature

import id.ac.loopy.feature.auth.AuthFeedback
import id.ac.loopy.feature.auth.authHttpFeedback
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AuthFeedbackTest {
    @Test
    fun registrationValidationUsesLocalMessagesForKnownFields() {
        val response = failure(
            422,
            """{"error":{"fieldErrors":{"password":"Use 8–128 characters (at most 512 bytes).","unknown":"Internal error"}}}""",
        )

        assertEquals(
            AuthFeedback(fields = mapOf("password" to "Coba gunakan frasa dari beberapa kata sebagai kata sandi.")),
            authHttpFeedback(response, registration = true),
        )
    }

    @Test
    fun loginFailureAndDuplicateRegistrationHaveDifferentDestinations() {
        assertEquals(
            "Email atau kata sandi salah. Periksa kembali, lalu coba lagi.",
            authHttpFeedback(failure(401), registration = false).message,
        )
        assertEquals(
            "Email ini sudah terdaftar. Masuk atau gunakan email lain.",
            authHttpFeedback(failure(409), registration = true).fields["email"],
        )
        assertTrue(authHttpFeedback(failure(409), registration = false).fields.isEmpty())
    }

    @Test
    fun rateLimitingAndServerBusyUseDifferentExplanations() {
        assertEquals(
            "Terlalu banyak percobaan dalam waktu singkat. Tunggu sebentar, lalu coba lagi.",
            authHttpFeedback(failure(429, """{"error":{"code":"RATE_LIMITED"}}"""), registration = false).message,
        )
        assertEquals(
            "Layanan sedang sibuk. Tunggu sebentar, lalu coba lagi.",
            authHttpFeedback(failure(429, """{"error":{"code":"AUTH_BUSY"}}"""), registration = false).message,
        )
    }

    @Test
    fun invalidResponsesUseActionableFallbacks() {
        assertEquals(
            "Periksa nama, email, dan kata sandi, lalu coba lagi.",
            authHttpFeedback(failure(422, "invalid{"), registration = true).message,
        )
        assertEquals(
            "Layanan sedang bermasalah. Coba lagi sebentar.",
            authHttpFeedback(failure(503, "Database connection refused"), registration = false).message,
        )
    }

    private fun failure(status: Int, body: String = "{}"): HttpException =
        HttpException(Response.error<Unit>(status, body.toResponseBody()))
}
