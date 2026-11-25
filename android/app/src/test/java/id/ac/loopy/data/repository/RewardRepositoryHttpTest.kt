package id.ac.loopy.data.repository

import id.ac.loopy.data.remote.api.AuthApi
import id.ac.loopy.data.remote.api.RewardApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RewardRepositoryHttpTest {
    @Test fun retrofitContractRefreshesBearerAndPreservesRedemptionKeyAcrossHttpRetry() = runBlocking {
        val server = MockWebServer()
        val redemption = """{"redemptionId":"red-1","rewardId":"api-reward","rewardName":"Catalog from server","description":"Digital badge","icon":"leaf","type":"BADGE","pointsUsed":10,"redeemedAt":"2026-11-25T02:30:00.123456789Z"}"""
        fun reply(body: String, status: Int = 200) {
            server.enqueue(MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body))
        }
        fun session(token: String) = """{"accessToken":"$token","refreshToken":"refresh","user":{"userId":"u1","name":"API User","role":"USER"}}"""
        reply(session("old"))
        reply("""{"error":{"code":"UNAUTHORIZED"}}""", 401)
        reply(session("new"))
        reply("""{"balance":42,"updatedAt":"2026-11-25T02:30:00Z"}""")
        reply("""{"items":[{"rewardId":"api-reward","name":"Catalog from server","description":"Digital badge","icon":"leaf","requiredPoints":10,"type":"BADGE","isActive":true}]}""")
        reply("""{"error":{"code":"INTERNAL_ERROR"}}""", 500)
        reply("""{"redemption":$redemption,"remainingBalance":32,"replayed":true}""")
        reply("""{"items":[$redemption]}""")
        server.start()
        try {
            val retrofit = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build()
            val auth = AuthRepositoryImpl(retrofit.create(AuthApi::class.java))
            val repo = RewardRepositoryImpl(retrofit.create(RewardApi::class.java), auth)
            assertTrue(runCatching { repo.balance() }.exceptionOrNull() is IllegalStateException)
            assertEquals(0, server.requestCount)
            auth.login("user@example.test", "test password")
            assertEquals(42, repo.balance())
            assertEquals("Catalog from server", repo.catalog().single().name)
            val key = "test-redemption-key"
            assertTrue(runCatching { repo.redeem("api-reward", key) }.exceptionOrNull() is HttpException)
            val result = repo.redeem("api-reward", key)
            assertTrue(result.replayed)
            assertEquals(32, result.remainingBalance)
            assertEquals(result.redemption, repo.history().single())

            val requests = List(8) { requireNotNull(server.takeRequest(1, TimeUnit.SECONDS)) }
            assertEquals(listOf("/v1/auth/login", "/v1/points/balance", "/v1/auth/refresh",
                "/v1/points/balance", "/v1/rewards", "/v1/redemptions", "/v1/redemptions", "/v1/redemptions"),
                requests.map { it.path })
            assertEquals("Bearer old", requests[1].getHeader("Authorization"))
            listOf(3, 4, 5, 6, 7).forEach { assertEquals("Bearer new", requests[it].getHeader("Authorization")) }
            listOf(5, 6).forEach {
                assertEquals("POST", requests[it].method)
                assertEquals(key, requests[it].getHeader("Idempotency-Key"))
                assertEquals("""{"rewardId":"api-reward"}""", requests[it].body.readUtf8())
            }
            assertEquals("""{"refreshToken":"refresh"}""", requests[2].body.readUtf8())
        } finally {
            server.shutdown()
        }
    }
}
