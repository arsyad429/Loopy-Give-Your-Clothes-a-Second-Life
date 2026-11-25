package id.ac.loopy.ui

import android.net.Uri
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.ac.loopy.BuildConfig
import id.ac.loopy.di.AppContainer
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.feature.admin.AdminDashboardScreen
import id.ac.loopy.feature.admin.AdminDashboardViewModel
import id.ac.loopy.feature.admin.AdminDonationDetailScreen
import id.ac.loopy.feature.admin.AdminDonationDetailViewModel
import id.ac.loopy.feature.admin.DonationRequestsScreen
import id.ac.loopy.feature.admin.DonationRequestsViewModel
import id.ac.loopy.feature.admin.SampleDonationReviews
import id.ac.loopy.feature.admin.formatPickupSchedule
import id.ac.loopy.feature.auth.LoginScreen
import id.ac.loopy.feature.auth.LoginViewModel
import id.ac.loopy.feature.auth.RegisterScreen
import id.ac.loopy.feature.clothingcheck.CheckResultScreen
import id.ac.loopy.feature.clothingcheck.CheckResultViewModel
import id.ac.loopy.feature.clothingcheck.ClothingDetectorScreen
import id.ac.loopy.feature.clothingcheck.ClothingDetectorViewModel
import id.ac.loopy.feature.donations.DonationDetailScreen
import id.ac.loopy.feature.donations.DonationDetailViewModel
import id.ac.loopy.feature.donations.MyDonationsScreen
import id.ac.loopy.feature.donations.MyDonationsViewModel
import id.ac.loopy.feature.home.HomeScreen
import id.ac.loopy.feature.landing.LandingScreen
import id.ac.loopy.feature.pickup.PickupRequestScreen
import id.ac.loopy.feature.pickup.PickupRequestViewModel
import id.ac.loopy.feature.profile.AdminProfileScreen
import id.ac.loopy.feature.profile.UserProfileScreen
import id.ac.loopy.feature.rewards.RewardsScreen
import id.ac.loopy.feature.rewards.CompletionViewModel
import id.ac.loopy.feature.rewards.CompletionUiState
import id.ac.loopy.feature.rewards.CompletionAdminScreen
import id.ac.loopy.feature.rewards.toAdminReview
import id.ac.loopy.ui.components.TemplateScreen
import id.ac.loopy.feature.settings.SettingsScreen
import id.ac.loopy.feature.verification.EmailVerificationScreen
import id.ac.loopy.ui.components.LoopyInfo
import id.ac.loopy.ui.components.LoopyScreen

private val PreviewScreens = listOf(
    "completion-admin" to "Demo E–F · Admin penyelesaian donasi & poin",
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
    "demo-admin-requests" to "A02 · Contoh antrean donasi",
    "demo-admin-review/LP024" to "A03 · Contoh verifikasi donasi",
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
    container: AppContainer,
    completion: CompletionViewModel? = null,
) {
    val nav = rememberNavController()
    val goBack: () -> Unit = { completion?.dismissMessage(); nav.popBackStack() }
    val clothingCheckRepository = container.clothingCheckRepository
    val donationRepository = container.donationRepository
    val adminRepository = container.adminRepository
    val authFactory = remember(container) {
        viewModelFactory { initializer { LoginViewModel(container.authRepository) } }
    }
    val authModel: LoginViewModel = viewModel(factory = authFactory)
    val session by authModel.session.collectAsStateWithLifecycle()
    val submitting by authModel.busy.collectAsStateWithLifecycle()
    val logoutError by authModel.logoutError.collectAsStateWithLifecycle()
    val loginFeedback by authModel.loginFeedback.collectAsStateWithLifecycle()
    val registerFeedback by authModel.registerFeedback.collectAsStateWithLifecycle()

    fun goToAccountScreen(role: String?) {
        val route = when (role) {
            "USER" -> "s04"
            "ADMIN" -> "a01"
            else -> "s02"
        }
        nav.navigate(route) {
            popUpTo(nav.graph.startDestinationId)
            launchSingleTop = true
        }
    }

    val logout: () -> Unit = { authModel.logout { goToAccountScreen(null) } }
    val completionState = completion?.state?.collectAsStateWithLifecycle()?.value ?: CompletionUiState()
    val userNavigate: (String) -> Unit = { route ->
        completion?.dismissMessage()
        nav.navigate(route) {
            popUpTo(if (BuildConfig.DEBUG) "templates" else "s04") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openDonation: (String) -> Unit = { nav.navigate("s09/$it") }
    val account: () -> Unit = { nav.navigate("s11") }
    val refreshCompletion: () -> Unit = { completion?.prepare() }

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

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                    val role = session?.user?.role
                    LaunchedEffect(role) {
                        if (role != null) goToAccountScreen(role)
                    }
                    LoginScreen(
                        onBack = {
                            authModel.clearFeedback()
                            goBack()
                        },
                        onRegister = {
                            authModel.clearFeedback()
                            nav.navigate("s03")
                        },
                        onLogin = authModel::login,
                        feedback = loginFeedback?.message,
                        isSubmitting = submitting,
                        onClearFeedback = authModel::clearFeedback,
                    )
                }

                composable("s03") {
                    val role = session?.user?.role
                    LaunchedEffect(role) {
                        if (role != null) goToAccountScreen(role)
                    }
                    RegisterScreen(
                        onBack = {
                            authModel.clearFeedback()
                            goBack()
                        },
                        onLogin = {
                            authModel.clearFeedback()
                            if (!nav.popBackStack("s02", inclusive = false)) nav.navigate("s02")
                        },
                        onRegister = authModel::register,
                        feedback = registerFeedback?.message,
                        fieldErrors = registerFeedback?.fields.orEmpty(),
                        isSubmitting = submitting,
                        onFieldEdited = authModel::clearRegisterFieldFeedback,
                    )
                }

                composable("s04") {
                    val user = session?.user
                    val role = user?.role
                    if (BuildConfig.DEBUG && completion != null && user == null) {
                        HomeScreen(completionState, userNavigate, openDonation, { nav.navigate("s05") }, account, refreshCompletion)
                    } else if (user?.role != "USER") {
                        LaunchedEffect(role) { goToAccountScreen(role) }
                    } else {
                        HomeScreen(
                            name = user.name,
                            onBack = goBack,
                            onCheckClothing = { nav.navigate("s05") },
                            onMyDonations = { nav.navigate("s08") },
                            onLogout = logout,
                            logoutBusy = submitting,
                            logoutError = logoutError,
                        )
                    }
                }
                composable("s05") { entry ->
                    val role = session?.user?.role
                    if (role != "USER") {
                        LaunchedEffect(role) { goToAccountScreen(role) }
                        return@composable
                    }
                    val factory = remember(clothingCheckRepository) {
                        viewModelFactory {
                            initializer { ClothingDetectorViewModel(clothingCheckRepository) }
                        }
                    }
                    val model = viewModel<ClothingDetectorViewModel>(
                        viewModelStoreOwner = entry,
                        factory = factory,
                    )

                    ClothingDetectorScreen(
                        viewModel = model,
                        onCheckReady = { checkId ->
                            nav.navigate("s06/${Uri.encode(checkId)}") {
                                launchSingleTop = true
                            }
                        },
                        onBack = goBack,
                    )
                }
                if (BuildConfig.DEBUG) composable("s06") {
                    CheckResultScreen(
                        viewModel = null,
                        onBack = goBack,
                        onRecheck = { nav.navigate("s05") },
                        onPickup = {},
                        emptyMessage = "Lakukan pemeriksaan pakaian untuk melihat hasilnya.",
                    )
                }
                composable(
                    route = "s06/{checkId}",
                    arguments = listOf(navArgument("checkId") { type = NavType.StringType }),
                ) { entry ->
                    val role = session?.user?.role
                    if (role != "USER") {
                        LaunchedEffect(role) { goToAccountScreen(role) }
                        return@composable
                    }
                    val checkId = entry.arguments?.getString("checkId").orEmpty()
                    val factory = remember(clothingCheckRepository, checkId) {
                        viewModelFactory {
                            initializer { CheckResultViewModel(checkId, clothingCheckRepository) }
                        }
                    }
                    val model = viewModel<CheckResultViewModel>(
                        viewModelStoreOwner = entry,
                        factory = factory,
                    )
                    CheckResultScreen(
                        viewModel = model,
                        loadPrivatePhoto = clothingCheckRepository::downloadPhoto,
                        onBack = goBack,
                        onRecheck = {
                            nav.navigate("s05") {
                                popUpTo("s05") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onPickup = { checkId ->
                            nav.navigate("s07/${Uri.encode(checkId)}") { launchSingleTop = true }
                        },
                    )
                }
                composable("s07") { entry ->
                    if (BuildConfig.DEBUG && completion != null && session == null) {
                        val demoRepo = container.completionDemoRepository
                        val factory = remember(demoRepo) {
                            viewModelFactory {
                                initializer {
                                    PickupRequestViewModel(
                                        checkId = "",
                                        donorName = "Arsyad",
                                        clothingCheckRepository = null,
                                        donationRepository = null,
                                        demoRepository = demoRepo,
                                    )
                                }
                            }
                        }
                        val model = viewModel<PickupRequestViewModel>(
                            viewModelStoreOwner = entry,
                            factory = factory,
                        )
                        PickupRequestScreen(
                            viewModel = model,
                            onBack = goBack,
                            onRecheck = { nav.navigate("s05") },
                            onGoToMyDonations = {
                                nav.navigate("s08") {
                                    popUpTo("templates") { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                        )
                    } else {
                        PickupRequestScreen(
                            viewModel = null,
                            onBack = goBack,
                            onRecheck = { nav.navigate("s05") },
                            onGoToMyDonations = { nav.navigate("s08") },
                        )
                    }
                }
                composable(
                    route = "s07/{checkId}",
                    arguments = listOf(navArgument("checkId") { type = NavType.StringType }),
                ) { entry ->
                    val user = session?.user
                    val role = user?.role
                    if (role != "USER") {
                        LaunchedEffect(role) { goToAccountScreen(role) }
                        return@composable
                    }
                    val checkId = entry.arguments?.getString("checkId").orEmpty()
                    val donorName = user.name
                    val factory = remember(clothingCheckRepository, donationRepository, checkId, donorName) {
                        viewModelFactory {
                            initializer {
                                PickupRequestViewModel(
                                    checkId = checkId,
                                    donorName = donorName,
                                    clothingCheckRepository = clothingCheckRepository,
                                    donationRepository = donationRepository,
                                )
                            }
                        }
                    }
                    val model = viewModel<PickupRequestViewModel>(
                        viewModelStoreOwner = entry,
                        factory = factory,
                    )
                    PickupRequestScreen(
                        viewModel = model,
                        loadPrivatePhoto = clothingCheckRepository::downloadPhoto,
                        onBack = goBack,
                        onRecheck = {
                            nav.navigate("s05") {
                                popUpTo("s05") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onGoToMyDonations = {
                            nav.navigate("s08") {
                                popUpTo("s04") { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable("s08") { entry ->
                    val role = session?.user?.role
                    if (BuildConfig.DEBUG && completion != null && session == null) {
                        MyDonationsScreen(completionState, userNavigate, openDonation, account, refreshCompletion)
                    } else if (role != "USER") {
                        LaunchedEffect(role) { goToAccountScreen(role) }
                    } else {
                        val factory = remember(donationRepository) {
                            viewModelFactory {
                                initializer { MyDonationsViewModel(donationRepository) }
                            }
                        }
                        val model = viewModel<MyDonationsViewModel>(
                            viewModelStoreOwner = entry,
                            factory = factory,
                        )
                        MyDonationsScreen(
                            viewModel = model,
                            onBack = goBack,
                            onOpen = openDonation,
                            onCheckClothing = { nav.navigate("s05") },
                        )
                    }
                }
                composable("s09") {
                    if (BuildConfig.DEBUG && completion != null && session == null) DonationDetailScreen(completionState.donations.firstOrNull(), goBack, userNavigate)
                    else TemplateScreen("Detail donasi", "Pilih donasi dari daftar Donasiku.", goBack)
                }
                composable("s09/{id}") { entry ->
                    val role = session?.user?.role
                    if (BuildConfig.DEBUG && completion != null && session == null) {
                        DonationDetailScreen(completionState.donations.find { it.id == entry.arguments?.getString("id") }, goBack, userNavigate)
                    } else if (role != "USER") {
                        LaunchedEffect(role) { goToAccountScreen(role) }
                    } else {
                        val donationId = entry.arguments?.getString("id").orEmpty()
                        val factory = remember(donationRepository, donationId) {
                            viewModelFactory {
                                initializer { DonationDetailViewModel(donationId, donationRepository) }
                            }
                        }
                        val model = viewModel<DonationDetailViewModel>(
                            viewModelStoreOwner = entry,
                            factory = factory,
                        )
                        DonationDetailScreen(viewModel = model, onBack = goBack)
                    }
                }
                composable("s10") {
                    if (BuildConfig.DEBUG && completion != null && session == null) RewardsScreen(completionState, userNavigate, account, completion::redeem, refreshCompletion, completion::dismissMessage)
                    else TemplateScreen("Rewards", "Integrasi donasi dan reward ke akun aktif belum terhubung.", goBack)
                }
                composable("a01") { entry ->
                    val admin = session?.user
                    if (admin?.role != "ADMIN") {
                        LaunchedEffect(admin?.role) { goToAccountScreen(admin?.role) }
                        return@composable
                    }
                    val factory = remember(adminRepository) {
                        viewModelFactory { initializer { AdminDashboardViewModel(adminRepository) } }
                    }
                    val model = viewModel<AdminDashboardViewModel>(entry, key = admin.userId, factory = factory)
                    AdminDashboardScreen(
                        model = model,
                        name = admin.name,
                        onOpenRequests = { nav.navigate("a02") },
                        onLogout = logout,
                        logoutBusy = submitting,
                        logoutError = logoutError,
                    )
                }
                composable("a02") { entry ->
                    val admin = session?.user
                    if (admin?.role != "ADMIN") {
                        LaunchedEffect(admin?.role) { goToAccountScreen(admin?.role) }
                        return@composable
                    }
                    val factory = remember(adminRepository) {
                        viewModelFactory { initializer { DonationRequestsViewModel(adminRepository) } }
                    }
                    val model = viewModel<DonationRequestsViewModel>(entry, key = admin.userId, factory = factory)
                    DonationRequestsScreen(model, goBack) { id -> nav.navigate("a03/${Uri.encode(id)}") }
                }
                composable(
                    route = "a03/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { entry ->
                    val admin = session?.user
                    if (admin?.role != "ADMIN") {
                        LaunchedEffect(admin?.role) { goToAccountScreen(admin?.role) }
                        return@composable
                    }
                    val id = entry.arguments?.getString("id").orEmpty()
                    val factory = remember(container, id, admin.userId) {
                        viewModelFactory {
                            initializer {
                                AdminDonationDetailViewModel(id, adminRepository, admin.userId, container.adminPickupScheduleDrafts)
                            }
                        }
                    }
                    val model = viewModel<AdminDonationDetailViewModel>(entry, key = admin.userId, factory = factory)
                    AdminDonationDetailScreen(model, clothingCheckRepository::downloadPhoto, goBack)
                }
                composable("s11") { UserProfileScreen(goBack) }
                composable("s12") { SettingsScreen(goBack) }
                composable("a04") { AdminProfileScreen(goBack) }

                if (BuildConfig.DEBUG) {
                    composable("completion-admin") {
                        CompletionAdminScreen(completionState, goBack, { nav.navigate("completion-admin/$it") }, refreshCompletion)
                    }
                    composable("completion-admin/{id}") { entry ->
                        val donation = completionState.donations.find { it.id == entry.arguments?.getString("id") }
                        if (donation == null) Text("Pengajuan tidak ditemukan.")
                        else AdminDonationDetailScreen(
                            review = donation.toAdminReview(), onBack = goBack, onSchedule = {}, onReject = {},
                            onAdvance = { completion?.advance(donation.id, it) }, busy = completionState.busy,
                            completionMessage = completionState.message, completionError = completionState.error,
                        )
                    }
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

                    composable("demo-admin-requests") {
                        DonationRequestsScreen(
                            reviews = previews,
                            onBack = goBack,
                            onOpen = { id -> nav.navigate("demo-admin-review/$id") },
                        )
                    }

                    composable("demo-admin-review/{id}") { entry ->
                        val review = previews.firstOrNull { it.id == entry.arguments?.getString("id") }

                        if (review == null) {
                            LoopyScreen(title = "Verifikasi donasi", onBack = goBack) {
                                LoopyInfo(
                                    title = "Pengajuan tidak ditemukan",
                                    description = "Kembali ke antrean untuk memilih pengajuan lain.",
                                    isError = true,
                                )
                            }
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
                            completion?.dismissMessage()
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
