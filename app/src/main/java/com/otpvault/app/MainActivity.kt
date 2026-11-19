package com.otpvault.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import com.otpvault.app.core.preferences.AppPreferences
import com.otpvault.app.core.preferences.ThemeMode
import com.otpvault.app.core.security.AppLockManager
import com.otpvault.app.presentation.navigation.AppNavigation
import com.otpvault.app.presentation.theme.OtpVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var prefs: AppPreferences
    @Inject lateinit var lockManager: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FLAG_SECURE: block screenshots / screen recording and hide content
        // in the recent-apps preview on sensitive screens (applied app-wide).
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        CoroutineScope(Dispatchers.Main).launch {
            val lang = prefs.language.first()
            if (lang != "system") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
            }
            lockManager.autoLockTimeoutMs = prefs.autoLockSeconds.first() * 1000L
            lockManager.lockOnBackground = prefs.lockOnBackground.first()
        }

        setContent {
            val theme by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            OtpVaultTheme(mode = theme) {
                AppNavigation()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Lock immediately when the app enters the background (when enabled).
        lockManager.onBackground()
    }

    override fun onStart() {
        super.onStart()
        lockManager.onForeground()
    }
}
