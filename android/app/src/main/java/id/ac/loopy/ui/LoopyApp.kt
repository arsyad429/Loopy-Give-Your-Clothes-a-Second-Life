package id.ac.loopy.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.ac.loopy.BuildConfig
import id.ac.loopy.domain.repository.ClothingCheckRepository
import id.ac.loopy.feature.landing.LandingScreen
import id.ac.loopy.feature.auth.LoginScreen
import id.ac.loopy.feature.auth.RegisterScreen
import id.ac.loopy.feature.home.HomeScreen
import id.ac.loopy.feature.clothingcheck.ClothingDetectorScreen
import id.ac.loopy.feature.clothingcheck.ClothingDetectorViewModel
import id.ac.loopy.feature.clothingcheck.CheckResultScreen
import id.ac.loopy.feature.clothingcheck.CheckResultViewModel
import id.ac.loopy.feature.pickup.PickupRequestScreen
import id.ac.loopy.feature.donations.MyDonationsScreen
import id.ac.loopy.feature.donations.DonationDetailScreen
import id.ac.loopy.feature.rewards.RewardsScreen
import id.ac.loopy.feature.admin.AdminDashboardScreen
import id.ac.loopy.feature.admin.DonationRequestsScreen
import id.ac.loopy.feature.admin.AdminDonationDetailScreen
import id.ac.loopy.feature.profile.UserProfileScreen
import id.ac.loopy.feature.settings.SettingsScreen
import id.ac.loopy.feature.profile.AdminProfileScreen
import id.ac.loopy.feature.verification.EmailVerificationScreen

/** Harness debug untuk menelusuri template, bukan alur login atau otorisasi aplikasi final. */
@Composable
fun LoopyApp(
    state: TemplateUiState,
    onTheme: (Boolean) -> Unit,
    onHealth: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    clothingCheckRepository: ClothingCheckRepository? = null,
) {
    val nav = rememberNavController()
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.safeDrawingPadding()) {
            NavHost(navController = nav, startDestination = if (BuildConfig.DEBUG) "templates" else "s01") {
        composable("s01") { LandingScreen { nav.popBackStack() } }
        composable("s02") { LoginScreen { nav.popBackStack() } }
        composable("s03") { RegisterScreen { nav.popBackStack() } }
        composable("s04") { HomeScreen { nav.popBackStack() } }
        composable("s05") { entry ->
            val model = clothingCheckRepository?.let { repository ->
                val factory = remember(repository) {
                    viewModelFactory {
                        initializer { ClothingDetectorViewModel(repository) }
                    }
                }
                viewModel<ClothingDetectorViewModel>(
                    viewModelStoreOwner = entry,
                    factory = factory,
                )
            }

            ClothingDetectorScreen(
                viewModel = model,
                onCheckReady = { checkId ->
                    nav.navigate("s06/${Uri.encode(checkId)}") {
                        launchSingleTop = true
                    }
                },
                onBack = { nav.popBackStack() },
            )
        }
        if (BuildConfig.DEBUG) composable("s06") {
            CheckResultScreen(
                viewModel = null,
                onBack = { nav.popBackStack() },
                onRecheck = { nav.navigate("s05") },
                onPickup = {},
                emptyMessage = "Lakukan pemeriksaan pakaian untuk melihat hasilnya.",
            )
        }
        composable(
            route = "s06/{checkId}",
            arguments = listOf(navArgument("checkId") { type = NavType.StringType }),
        ) { entry ->
            val model = clothingCheckRepository?.let { repository ->
                val checkId = entry.arguments?.getString("checkId").orEmpty()
                val factory = remember(repository, checkId) {
                    viewModelFactory {
                        initializer { CheckResultViewModel(checkId, repository) }
                    }
                }
                viewModel<CheckResultViewModel>(viewModelStoreOwner = entry, factory = factory)
            }
            CheckResultScreen(
                viewModel = model,
                onBack = { nav.popBackStack() },
                onRecheck = {
                    if (!nav.popBackStack("s05", inclusive = false)) nav.navigate("s05")
                },
                onPickup = { checkId ->
                    nav.navigate("s07/${Uri.encode(checkId)}") { launchSingleTop = true }
                },
            )
        }
        composable("s07") { PickupRequestScreen { nav.popBackStack() } }
        composable(
            route = "s07/{checkId}",
            arguments = listOf(navArgument("checkId") { type = NavType.StringType }),
        ) {
            PickupRequestScreen { nav.popBackStack() }
        }
        composable("s08") { MyDonationsScreen { nav.popBackStack() } }
        composable("s09") { DonationDetailScreen { nav.popBackStack() } }
        composable("s10") { RewardsScreen { nav.popBackStack() } }
        composable("a01") { AdminDashboardScreen { nav.popBackStack() } }
        composable("a02") { DonationRequestsScreen { nav.popBackStack() } }
        composable("a03") { AdminDonationDetailScreen { nav.popBackStack() } }
        composable("s11") { UserProfileScreen { nav.popBackStack() } }
        composable("s12") { SettingsScreen { nav.popBackStack() } }
        composable("a04") { AdminProfileScreen { nav.popBackStack() } }
        composable("s13") { EmailVerificationScreen { nav.popBackStack() } }
                if (BuildConfig.DEBUG) composable("templates") {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 20.dp)) {
                        item { Text("Loopy", style = MaterialTheme.typography.headlineLarge) }
                        item { Text("Give Your Clothes a Second Life!") }
                        item { Text("Project template · fondasi PAPB aktif, fitur PRD masih TODO.") }
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
                        item { Text("13 layar PRD + 4 perluasan desain", style = MaterialTheme.typography.titleMedium) }
            item { OutlinedButton(onClick = { nav.navigate("s01") }, modifier = Modifier.fillMaxWidth()) { Text("S01 · Landing Page") } }
            item { OutlinedButton(onClick = { nav.navigate("s02") }, modifier = Modifier.fillMaxWidth()) { Text("S02 · Login") } }
            item { OutlinedButton(onClick = { nav.navigate("s03") }, modifier = Modifier.fillMaxWidth()) { Text("S03 · Register") } }
            item { OutlinedButton(onClick = { nav.navigate("s04") }, modifier = Modifier.fillMaxWidth()) { Text("S04 · Home Dashboard") } }
            item { OutlinedButton(onClick = { nav.navigate("s05") }, modifier = Modifier.fillMaxWidth()) { Text("S05 · AI Clothing Detector") } }
            item { OutlinedButton(onClick = { nav.navigate("s06") }, modifier = Modifier.fillMaxWidth()) { Text("S06 · Clothing Check Result") } }
            item { OutlinedButton(onClick = { nav.navigate("s07") }, modifier = Modifier.fillMaxWidth()) { Text("S07 · Pickup Request Form") } }
            item { OutlinedButton(onClick = { nav.navigate("s08") }, modifier = Modifier.fillMaxWidth()) { Text("S08 · My Donations") } }
            item { OutlinedButton(onClick = { nav.navigate("s09") }, modifier = Modifier.fillMaxWidth()) { Text("S09 · Donation Detail") } }
            item { OutlinedButton(onClick = { nav.navigate("s10") }, modifier = Modifier.fillMaxWidth()) { Text("S10 · Rewards & My Rewards") } }
            item { OutlinedButton(onClick = { nav.navigate("a01") }, modifier = Modifier.fillMaxWidth()) { Text("A01 · Admin Dashboard") } }
            item { OutlinedButton(onClick = { nav.navigate("a02") }, modifier = Modifier.fillMaxWidth()) { Text("A02 · Donation Requests") } }
            item { OutlinedButton(onClick = { nav.navigate("a03") }, modifier = Modifier.fillMaxWidth()) { Text("A03 · Admin Donation Detail") } }
            item { OutlinedButton(onClick = { nav.navigate("s11") }, modifier = Modifier.fillMaxWidth()) { Text("S11 · Profil User") } }
            item { OutlinedButton(onClick = { nav.navigate("s12") }, modifier = Modifier.fillMaxWidth()) { Text("S12 · Settings") } }
            item { OutlinedButton(onClick = { nav.navigate("a04") }, modifier = Modifier.fillMaxWidth()) { Text("A04 · Profil Admin") } }
            item { OutlinedButton(onClick = { nav.navigate("s13") }, modifier = Modifier.fillMaxWidth()) { Text("S13 · Verifikasi Email") } }
                    }
                }
            }
        }
    }
}
