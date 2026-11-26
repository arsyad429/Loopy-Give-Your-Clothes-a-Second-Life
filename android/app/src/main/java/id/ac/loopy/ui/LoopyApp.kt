package id.ac.loopy.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.ac.loopy.di.AppContainer
import id.ac.loopy.feature.admin.AdminDashboardScreen
import id.ac.loopy.feature.admin.AdminDashboardViewModel
import id.ac.loopy.feature.admin.AdminDonationDetailScreen
import id.ac.loopy.feature.admin.AdminDonationDetailViewModel
import id.ac.loopy.feature.admin.DonationRequestsScreen
import id.ac.loopy.feature.admin.DonationRequestsViewModel
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
import id.ac.loopy.feature.home.HomeViewModel
import id.ac.loopy.feature.landing.LandingScreen
import id.ac.loopy.feature.pickup.PickupRequestScreen
import id.ac.loopy.feature.pickup.PickupRequestViewModel
import id.ac.loopy.feature.profile.AccountViewModel
import id.ac.loopy.feature.profile.AdminProfileScreen
import id.ac.loopy.feature.profile.UserProfileScreen
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import id.ac.loopy.feature.rewards.RewardsViewModel
import id.ac.loopy.feature.rewards.RewardsScreen
import id.ac.loopy.ui.components.TemplateScreen
import id.ac.loopy.feature.settings.SettingsScreen
import id.ac.loopy.feature.verification.EmailVerificationScreen

@Composable
fun LoopyApp(container: AppContainer) {
    val nav = rememberNavController()
    val goBack: () -> Unit = { nav.popBackStack() }
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
    val emailRegistration by authModel.emailRegistration.collectAsStateWithLifecycle()
    val profileFactory = remember(container) {
        viewModelFactory { initializer { AccountViewModel(container.authRepository) } }
    }

    fun goToAccountScreen(role: String?) {
        val route = when (role) {
            "USER" -> "s04"
            "ADMIN" -> "a01"
            else -> "s02"
        }
        if (role == null) {
            nav.clearBackStack("s08")
            nav.clearBackStack("s10")
        }
        nav.navigate(route) {
            popUpTo(nav.graph.startDestinationId)
            launchSingleTop = true
        }
    }

    val logout: () -> Unit = { authModel.logout { goToAccountScreen(null) } }
    val userNavigate: (String) -> Unit = nav::navigateToUserTab
    val openDonation: (String) -> Unit = { id -> nav.navigate("s09/${Uri.encode(id)}") { launchSingleTop = true } }
    val account: () -> Unit = { nav.navigate("s11") { launchSingleTop = true } }
    val adminAccount: () -> Unit = { nav.navigate("a04") { launchSingleTop = true } }
    val settings: () -> Unit = { nav.navigate("s12") { launchSingleTop = true } }
    val adminNavigate: (String) -> Unit = { route ->
        nav.navigate(route) {
            popUpTo("a01")
            launchSingleTop = true
        }
    }
    val openAdminDonation: (String) -> Unit = { id -> nav.navigate("a03/${Uri.encode(id)}") { launchSingleTop = true } }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding()) {
            NavHost(navController = nav, startDestination = "s01") {
                composable("s01") {
                    LandingScreen(
                        onLogin = { nav.navigate("s02") },
                        onRegister = { nav.navigate("s03") },
                        onBack = null,
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
                    val challengeId = emailRegistration?.challenge?.challengeId
                    LaunchedEffect(role, challengeId) {
                        if (role != null) goToAccountScreen(role)
                        else if (challengeId != null) nav.navigate("s13") { launchSingleTop = true }
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

                composable("s13") {
                    val role = session?.user?.role
                    val state = emailRegistration
                    when {
                        role != null -> LaunchedEffect(role) { goToAccountScreen(role) }
                        state == null -> LaunchedEffect(Unit) {
                            if (!nav.popBackStack("s03", inclusive = false)) {
                                nav.navigate("s03") { popUpTo("s13") { inclusive = true } }
                            }
                        }
                        else -> EmailVerificationScreen(
                            state = state,
                            busy = submitting,
                            onBack = {
                                authModel.cancelEmailRegistration()
                                goBack()
                            },
                            onVerify = authModel::verifyRegistration,
                            onResend = authModel::resendRegistration,
                            onCodeEdit = authModel::clearCodeError,
                        )
                    }
                }

                composable("s04") { entry ->
                    val user = session?.user
                    val role = user?.role
                    if (user?.role != "USER") {
                        LaunchedEffect(role) { goToAccountScreen(role) }
                    } else {
                        val repository = container.homeRepository
                        val factory = remember(repository) {
                            viewModelFactory { initializer { HomeViewModel(repository) } }
                        }
                        val model = viewModel<HomeViewModel>(entry, key = user.userId, factory = factory)
                        HomeScreen(
                            viewModel = model,
                            name = user.name,
                            onCheckClothing = { nav.navigate("s05") { launchSingleTop = true } },
                            onMyDonations = { nav.navigate("s08") { launchSingleTop = true } },
                            onRewards = { nav.navigate("s10") { launchSingleTop = true } },
                            onDonation = openDonation,
                            onProfile = account,
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
                composable("s07") {
                    val role = session?.user?.role
                    if (role != "USER") {
                        LaunchedEffect(role) { goToAccountScreen(role) }
                    } else {
                        PickupRequestScreen(
                            viewModel = null, onBack = goBack,
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
                    if (role != "USER") {
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
                            onNavigate = userNavigate,
                            onAccount = account,
                        )
                    }
                }
                composable("s09") {
                    TemplateScreen("Detail donasi", "Pilih donasi dari daftar Donasiku.", goBack)
                }
                composable("s09/{id}") { entry ->
                    val role = session?.user?.role
                    if (role != "USER") {
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
                        DonationDetailScreen(viewModel = model, onBack = goBack, onNavigate = userNavigate)
                    }
                }
                composable("s10") { entry ->
                    val user = session?.user
                    if (user?.role != "USER") {
                        LaunchedEffect(user?.role) { goToAccountScreen(user?.role) }
                    } else {
                        val factory = remember(container, user.userId) {
                            viewModelFactory { initializer {
                                RewardsViewModel(container.rewardRepository, createSavedStateHandle(), user.userId)
                            } }
                        }
                        val model = viewModel<RewardsViewModel>(entry, key = user.userId, factory = factory)
                        val rewards by model.state.collectAsStateWithLifecycle()
                        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refresh() }
                        RewardsScreen(
                            rewards, userNavigate, account, model::select, model::redeem,
                            model::refresh, model::dismissSelection, model::resumePending,
                        )
                    }
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
                        onOpenRequests = { adminNavigate("a02") },
                        onOpenDonation = openAdminDonation,
                        onNavigate = adminNavigate,
                        onAccount = adminAccount,
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
                    DonationRequestsScreen(model, goBack, adminNavigate, adminAccount, openAdminDonation)
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
                composable("s11") { entry ->
                    val user = session?.user
                    if (user?.role != "USER") {
                        LaunchedEffect(user?.role) { goToAccountScreen(user?.role) }
                        return@composable
                    }
                    val model = viewModel<AccountViewModel>(entry, key = user.userId, factory = profileFactory)
                    UserProfileScreen(model, goBack, settings, { userNavigate("s10") }, logout, submitting, logoutError)
                }
                composable("a04") { entry ->
                    val admin = session?.user
                    if (admin?.role != "ADMIN") {
                        LaunchedEffect(admin?.role) { goToAccountScreen(admin?.role) }
                        return@composable
                    }
                    val model = viewModel<AccountViewModel>(entry, key = admin.userId, factory = profileFactory)
                    AdminProfileScreen(model, goBack, settings, { adminNavigate("a01") }, logout, submitting, logoutError)
                }
                composable("s12") { entry ->
                    val user = session?.user
                    if (user == null) {
                        LaunchedEffect(Unit) { goToAccountScreen(null) }
                        return@composable
                    }
                    val model = viewModel<AccountViewModel>(entry, key = user.userId, factory = profileFactory)
                    SettingsScreen(model, goBack)
                }

            }
        }
    }
}

internal fun NavController.navigateToUserTab(route: String) {
    navigate(route) {
        popUpTo("s04") { saveState = true }
        launchSingleTop = true
        // Beranda is already in the stack. Restoring its saved state can reopen the previous tab.
        restoreState = route != "s04"
    }
}
