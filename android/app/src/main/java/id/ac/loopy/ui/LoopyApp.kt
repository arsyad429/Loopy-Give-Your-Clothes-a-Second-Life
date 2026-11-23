package id.ac.loopy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.ac.loopy.BuildConfig
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.feature.admin.AdminDashboardScreen
import id.ac.loopy.feature.admin.AdminDonationDetailScreen
import id.ac.loopy.feature.admin.DonationRequestsScreen
import id.ac.loopy.feature.admin.SampleDonationReviews
import id.ac.loopy.feature.admin.formatPickupSchedule
import id.ac.loopy.feature.auth.LoginScreen
import id.ac.loopy.feature.auth.RegisterScreen
import id.ac.loopy.feature.clothingcheck.CheckResultScreen
import id.ac.loopy.feature.clothingcheck.ClothingDetectorScreen
import id.ac.loopy.feature.donations.DonationDetailScreen
import id.ac.loopy.feature.donations.MyDonationsScreen
import id.ac.loopy.feature.home.HomeScreen
import id.ac.loopy.feature.landing.LandingScreen
import id.ac.loopy.feature.pickup.PickupRequestScreen
import id.ac.loopy.feature.profile.AdminProfileScreen
import id.ac.loopy.feature.profile.UserProfileScreen
import id.ac.loopy.feature.rewards.RewardsScreen
import id.ac.loopy.feature.settings.SettingsScreen
import id.ac.loopy.feature.verification.EmailVerificationScreen

private val PreviewScreens = listOf(
    "s01" to "S01 · Landing Page",
    "s02" to "S02 · Login",
    "s03" to "S03 · Register",
    "s04" to "S04 · Home Dashboard",
    "s05" to "S05 · AI Clothing Detector",
    "s06" to "S06 · Clothing Check Result",
    "s07" to "S07 · Pickup Request Form",
    "s08" to "S08 · My Donations",
    "s09" to "S09 · Donation Detail",
    "s10" to "S10 · Rewards & My Rewards",
    "a01" to "A01 · Admin Dashboard",
    "a02" to "A02 · Donation Requests",
    "a03/LP024" to "A03 · Admin Donation Detail",
    "s11" to "S11 · Profil User",
    "s12" to "S12 · Settings",
    "a04" to "A04 · Profil Admin",
    "s13" to "S13 · Verifikasi Email",
)

@Composable
fun LoopyApp(
    state: TemplateUiState,
    onTheme: (Boolean) -> Unit,
    onHealth: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    val nav = rememberNavController()
    val goBack: () -> Unit = { nav.popBackStack() }

    var verificationEmail by rememberSaveable { mutableStateOf("a***@email.com") }
    var schedules by rememberSaveable { mutableStateOf(hashMapOf<String, Long>()) }
    var rejections by rememberSaveable { mutableStateOf(hashMapOf<String, String>()) }

    val previews = SampleDonationReviews.map { sample ->
        val scheduledAt = schedules[sample.id]
        val reason = rejections[sample.id]

        when {
            scheduledAt != null -> sample.copy(
                status = DonationStatus.PICKUP_SCHEDULED,
                schedule = formatPickupSchedule(scheduledAt),
            )
            reason != null -> sample.copy(status = DonationStatus.REJECTED, rejectionReason = reason)
            else -> sample
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.safeDrawingPadding()) {
            NavHost(navController = nav, startDestination = if (BuildConfig.DEBUG) "templates" else "s01") {
                composable("s01") {
                    LandingScreen(
                        onLogin = { nav.navigate("s02") },
                        onRegister = { nav.navigate("s03") },
                        onBack = goBack.takeIf { BuildConfig.DEBUG },
                    )
                }

                composable("s02") {
                    var feedback by rememberSaveable { mutableStateOf<String?>(null) }

                    LoginScreen(
                        onBack = goBack,
                        onRegister = { nav.navigate("s03") },
                        onLogin = { _, _ -> feedback = "Layanan masuk belum terhubung." },
                        feedback = feedback,
                    )
                }

                composable("s03") {
                    var feedback by rememberSaveable { mutableStateOf<String?>(null) }

                    RegisterScreen(
                        onBack = goBack,
                        onLogin = {
                            if (!nav.popBackStack("s02", inclusive = false)) nav.navigate("s02")
                        },
                        onRegister = { _, email, _ ->
                            if (BuildConfig.DEBUG) {
                                verificationEmail = "${email.first()}***@${email.substringAfter('@')}"
                                nav.navigate("s13")
                            } else {
                                feedback = "Layanan pendaftaran belum terhubung."
                            }
                        },
                        feedback = feedback,
                    )
                }

                composable("s04") { HomeScreen(goBack) }
                composable("s05") { ClothingDetectorScreen(goBack) }
                composable("s06") { CheckResultScreen(goBack) }
                composable("s07") { PickupRequestScreen(goBack) }
                composable("s08") { MyDonationsScreen(goBack) }
                composable("s09") { DonationDetailScreen(goBack) }
                composable("s10") { RewardsScreen(goBack) }
                composable("a01") { AdminDashboardScreen(goBack) }
                composable("s11") { UserProfileScreen(goBack) }
                composable("s12") { SettingsScreen(goBack) }
                composable("a04") { AdminProfileScreen(goBack) }

                if (BuildConfig.DEBUG) {
                    composable("s13") {
                        EmailVerificationScreen(
                            maskedEmail = verificationEmail,
                            onBack = goBack,
                            onEditEmail = {
                                if (!nav.popBackStack("s03", inclusive = false)) {
                                    nav.navigate("s03") { popUpTo("s13") { inclusive = true } }
                                }
                            },
                            onVerified = {
                                if (!nav.popBackStack("s02", inclusive = false)) {
                                    val previous = nav.previousBackStackEntry?.destination?.route
                                    nav.navigate("s02") {
                                        popUpTo(if (previous == "s03") "s03" else "s13") { inclusive = true }
                                    }
                                }
                            },
                        )
                    }

                    composable("a02") {
                        DonationRequestsScreen(
                            reviews = previews,
                            onBack = goBack,
                            onOpen = { id -> nav.navigate("a03/$id") },
                        )
                    }

                    composable("a03/{id}") { entry ->
                        val review = previews.firstOrNull { it.id == entry.arguments?.getString("id") }

                        if (review == null) {
                            Text("Pengajuan tidak ditemukan.")
                        } else {
                            AdminDonationDetailScreen(
                                review = review,
                                onBack = goBack,
                                onSchedule = { at ->
                                    schedules = HashMap(schedules).apply { put(review.id, at) }
                                },
                                onReject = { reason ->
                                    rejections = HashMap(rejections).apply { put(review.id, reason) }
                                },
                            )
                        }
                    }

                    composable("templates") {
                        TemplateMenu(state, onTheme, onHealth, onSave, onClear) { route ->
                            if (route == "s13") verificationEmail = "a***@email.com"
                            nav.navigate(route)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateMenu(
    state: TemplateUiState,
    onTheme: (Boolean) -> Unit,
    onHealth: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onOpen: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
    ) {
        item { Text("Loopy", style = MaterialTheme.typography.headlineLarge) }
        item { Text("Give Your Clothes a Second Life!") }
        item { Text("Pratinjau layar Loopy · Build debug") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tema gelap (DataStore)")
                Switch(checked = state.darkTheme, onCheckedChange = onTheme)
            }
        }

        item { Text("Room: ${state.draftCount} draft contoh lokal") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave) { Text("Simpan contoh") }
                OutlinedButton(onClick = onClear) { Text("Hapus contoh") }
            }
        }

        item { Text("Retrofit + Coroutines: ${state.health}") }
        item { Button(onClick = onHealth, enabled = !state.busy) { Text("Periksa backend") } }

        items(PreviewScreens, key = { it.first }) { (route, label) ->
            OutlinedButton(onClick = { onOpen(route) }, modifier = Modifier.fillMaxWidth()) {
                Text(label)
            }
        }
    }
}
