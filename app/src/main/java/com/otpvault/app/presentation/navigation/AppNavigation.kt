package com.otpvault.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.otpvault.app.R
import com.otpvault.app.core.security.BiometricHelper
import com.otpvault.app.presentation.screens.addotp.AddOtpScreen
import com.otpvault.app.presentation.screens.authentication.AuthViewModel
import com.otpvault.app.presentation.screens.backup.BackupRestoreScreen
import com.otpvault.app.presentation.screens.details.OtpDetailsScreen
import com.otpvault.app.presentation.screens.history.HistoryScreen
import com.otpvault.app.presentation.screens.home.HomeScreen
import com.otpvault.app.presentation.screens.onboarding.BiometricSetupScreen
import com.otpvault.app.presentation.screens.onboarding.OnboardingScreen
import com.otpvault.app.presentation.screens.onboarding.PinEntryScreen
import com.otpvault.app.presentation.screens.onboarding.SplashScreen
import com.otpvault.app.presentation.screens.search.SearchScreen
import com.otpvault.app.presentation.screens.services.ServicesScreen
import com.otpvault.app.presentation.screens.settings.AboutScreen
import com.otpvault.app.presentation.screens.settings.PrivacyPolicyScreen
import com.otpvault.app.presentation.screens.settings.SecuritySettingsScreen
import com.otpvault.app.presentation.screens.settings.SettingsScreen
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val CREATE_PIN = "create_pin"
    const val CONFIRM_PIN = "confirm_pin/{pin}"
    const val BIOMETRIC = "biometric_setup"
    const val UNLOCK = "unlock"
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ADD = "add_otp"
    const val EDIT = "edit_otp/{id}"
    const val DETAILS = "details/{id}"
    const val SEARCH = "search"
    const val SERVICES = "services"
    const val SECURITY = "security_settings"
    const val BACKUP = "backup"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"
    fun confirmPin(pin: String) = "confirm_pin/$pin"
    fun edit(id: String) = "edit_otp/$id"
    fun details(id: String) = "details/$id"
}

@Composable
fun AppNavigation(authViewModel: AuthViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val authState by authViewModel.state.collectAsState()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val context = LocalContext.current

    val mainRoutes = setOf(Routes.HOME, Routes.HISTORY, Routes.SETTINGS)
    val showChrome = currentRoute in mainRoutes

    Scaffold(
        bottomBar = {
            if (showChrome) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.HOME,
                        onClick = { navController.navigate(Routes.HOME) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.home)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.HISTORY,
                        onClick = { navController.navigate(Routes.HISTORY) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.History, contentDescription = null) },
                        label = { Text(stringResource(R.string.history)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.settings)) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (showChrome) {
                FloatingActionButton(onClick = { navController.navigate(Routes.ADD) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_otp))
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen()
                LaunchedEffect(Unit) {
                    delay(900)
                    val dest = when {
                        !authState.onboarded -> Routes.ONBOARDING
                        !authState.pinSet -> Routes.CREATE_PIN
                        else -> Routes.UNLOCK
                    }
                    navController.navigate(dest) { popUpTo(Routes.SPLASH) { inclusive = true } }
                }
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onDone = {
                    authViewModel.completeOnboarding()
                    navController.navigate(Routes.CREATE_PIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                })
            }
            composable(Routes.CREATE_PIN) {
                PinEntryScreen(
                    titleRes = R.string.create_pin, bodyRes = R.string.create_pin_body,
                    onPinComplete = { pin -> navController.navigate(Routes.confirmPin(pin)) }
                )
            }
            composable(
                Routes.CONFIRM_PIN,
                arguments = listOf(navArgument("pin") { type = NavType.StringType })
            ) { entry ->
                val firstPin = entry.arguments?.getString("pin") ?: ""
                var mismatch by remember { mutableStateOf(false) }
                PinEntryScreen(
                    titleRes = R.string.confirm_pin, bodyRes = R.string.confirm_pin_body,
                    error = if (mismatch) stringResource(R.string.pin_mismatch) else null,
                    onPinComplete = { pin ->
                        if (pin == firstPin) {
                            authViewModel.createPin(pin)
                            navController.navigate(Routes.BIOMETRIC) { popUpTo(Routes.CREATE_PIN) { inclusive = true } }
                        } else mismatch = true
                    }
                )
            }
            composable(Routes.BIOMETRIC) {
                BiometricSetupScreen(
                    onEnable = { navController.navigate(Routes.HOME) { popUpTo(navController.graph.id) { inclusive = true } } },
                    onSkip = { navController.navigate(Routes.HOME) { popUpTo(navController.graph.id) { inclusive = true } } }
                )
            }
            composable(Routes.UNLOCK) {
                var error by remember { mutableStateOf<String?>(null) }
                var attempts by remember { mutableIntStateOf(0) }
                PinEntryScreen(
                    titleRes = R.string.enter_pin, bodyRes = R.string.unlock,
                    error = error,
                    onPinComplete = { pin ->
                        if (authViewModel.unlockWithPin(pin)) {
                            navController.navigate(Routes.HOME) { popUpTo(navController.graph.id) { inclusive = true } }
                        } else {
                            attempts++
                            error = context.getString(R.string.pin_wrong)
                        }
                    }
                )
                LaunchedEffect(Unit) {
                    val activity = context as? FragmentActivity
                    if (activity != null && BiometricHelper.canAuthenticate(context)) {
                        BiometricHelper.authenticate(activity, "OTP Vault", context.getString(R.string.use_biometric),
                            onSuccess = {
                                authViewModel.unlockWithBiometric()
                                navController.navigate(Routes.HOME) { popUpTo(navController.graph.id) { inclusive = true } }
                            },
                            onError = { })
                    }
                }
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenDetails = { id -> navController.navigate(Routes.details(id)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onOpenDetails = { id -> navController.navigate(Routes.details(id)) })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenSecurity = { navController.navigate(Routes.SECURITY) },
                    onOpenBackup = { navController.navigate(Routes.BACKUP) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                    onOpenServices = { navController.navigate(Routes.SERVICES) }
                )
            }
            composable(Routes.ADD) {
                AddOtpScreen(onSaved = { navController.popBackStack() })
            }
            composable(
                Routes.EDIT,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                AddOtpScreen(editId = entry.arguments?.getString("id"), onSaved = { navController.popBackStack() })
            }
            composable(
                Routes.DETAILS,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("id") ?: ""
                OtpDetailsScreen(
                    otpId = id,
                    onDeleted = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.edit(it)) }
                )
            }
            composable(Routes.SEARCH) { SearchScreen(onOpenDetails = { id -> navController.navigate(Routes.details(id)) }) }
            composable(Routes.SERVICES) { ServicesScreen() }
            composable(Routes.SECURITY) { SecuritySettingsScreen() }
            composable(Routes.BACKUP) { BackupRestoreScreen() }
            composable(Routes.ABOUT) { AboutScreen() }
            composable(Routes.PRIVACY) { PrivacyPolicyScreen() }
        }
    }
}
