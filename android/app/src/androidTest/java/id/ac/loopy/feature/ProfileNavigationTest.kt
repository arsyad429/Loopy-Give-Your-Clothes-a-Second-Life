package id.ac.loopy.feature

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.ac.loopy.data.remote.api.AuthApi
import id.ac.loopy.data.repository.AuthRepositoryImpl
import id.ac.loopy.feature.profile.AccountViewModel
import id.ac.loopy.feature.profile.AdminProfileScreen
import id.ac.loopy.feature.profile.UserProfileScreen
import id.ac.loopy.ui.components.AdminBottomBar
import id.ac.loopy.ui.components.LoopyScreen
import id.ac.loopy.ui.components.UserBottomBar
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProfileNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun adminCanOpenRequestsAfterLeavingProfile() = checkNavigation("ADMIN")

    @Test fun donorCanOpenDonationsAfterLeavingProfile() = checkNavigation("USER")

    private fun checkNavigation(role: String) {
        val auth = AuthRepositoryImpl(profileApi(role))
        runBlocking { auth.login("profile@example.com", "fixture-password") }
        val model = AccountViewModel(auth)
        var logoutCalls = 0
        val isAdmin = role == "ADMIN"
        val homeRoute = if (isAdmin) "a01" else "s04"
        val profileRoute = if (isAdmin) "a04" else "s11"
        val listRoute = if (isAdmin) "a02" else "s08"
        val listTab = if (isAdmin) "Pengajuan" else "Donasiku"
        val listTitle = if (isAdmin) "Antrean donasi" else "Riwayat donasi"

        compose.setContent {
            MaterialTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = homeRoute) {
                    composable(homeRoute) {
                        LoopyScreen(
                            title = if (isAdmin) "Dashboard admin" else "Beranda donatur",
                            onAccount = { nav.navigate(profileRoute) },
                            bottomBar = {
                                if (isAdmin) AdminBottomBar(homeRoute) { nav.navigate(it) }
                                else UserBottomBar(homeRoute) { nav.navigate(it) }
                            },
                        ) {}
                    }
                    composable(profileRoute) {
                        val back: () -> Unit = { nav.popBackStack() }
                        val logout: () -> Unit = { logoutCalls++ }
                        if (isAdmin) {
                            AdminProfileScreen(model, back, {}, back, logout, logoutBusy = false, logoutError = null)
                        } else {
                            UserProfileScreen(model, back, {}, {}, logout, logoutBusy = false, logoutError = null)
                        }
                    }
                    composable(listRoute) {
                        LoopyScreen(title = listTitle, onBack = { nav.popBackStack() }) {}
                    }
                }
            }
        }

        compose.onNodeWithContentDescription("Akun").performClick()
        val logout = compose.onNodeWithText("Keluar dari akun").performScrollTo()
        val previousLogoutAction = checkNotNull(logout.fetchSemanticsNode().config[SemanticsActions.OnClick].action)
        compose.onNodeWithContentDescription("Kembali").performClick()
        compose.runOnIdle {
            previousLogoutAction()
            assertEquals(0, logoutCalls)
        }
        compose.onNodeWithText(listTab).performClick()
        compose.onNodeWithText(listTitle).assertExists()

        compose.onNodeWithContentDescription("Kembali").performClick()
        compose.onNodeWithContentDescription("Akun").performClick()
        compose.onNodeWithText("Keluar dari akun").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, logoutCalls) }
    }

    private fun profileApi(role: String): AuthApi {
        val user = """{"userId":"test-1","name":"Akun Uji","role":"$role","email":"profile@example.com"}"""
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val body = when (request.url.encodedPath) {
                "/v1/auth/login" -> """{"accessToken":"access","refreshToken":"refresh","user":$user}"""
                "/v1/auth/me" -> user
                else -> error("Unexpected fixture request: ${request.url.encodedPath}")
            }
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}
