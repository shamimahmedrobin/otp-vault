package com.otpvault.app.presentation.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.otpvault.app.R
import com.otpvault.app.core.security.BiometricHelper
import com.otpvault.app.presentation.components.PinDots

@Composable
fun SplashScreen() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🛡️", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        R.string.onboarding_title_1 to R.string.onboarding_body_1,
        R.string.onboarding_title_2 to R.string.onboarding_body_2,
        R.string.onboarding_title_3 to R.string.onboarding_body_3
    )
    val pager = rememberPagerState(pageCount = { pages.size })
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🔐", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(pages[page].first), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(pages[page].second), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.get_started))
        }
    }
}

@Composable
fun PinEntryScreen(
    titleRes: Int,
    bodyRes: Int,
    error: String? = null,
    onPinComplete: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(bodyRes), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        PinDots(filled = pin.length)
        Spacer(Modifier.height(16.dp))
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        PinPad(
            onDigit = { d ->
                if (pin.length < 6) {
                    pin += d
                    if (pin.length == 6) { onPinComplete(pin); pin = "" }
                }
            },
            onBackspace = { pin = pin.dropLast(1) }
        )
    }
}

@Composable
fun PinPad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { label ->
                    Button(
                        onClick = {
                            when (label) {
                                "⌫" -> onBackspace()
                                "" -> Unit
                                else -> onDigit(label)
                            }
                        },
                        enabled = label.isNotEmpty(),
                        modifier = Modifier.padding(4.dp)
                    ) { Text(label, style = MaterialTheme.typography.titleLarge) }
                }
            }
        }
    }
}

@Composable
fun BiometricSetupScreen(onEnable: () -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    val available = BiometricHelper.canAuthenticate(context)
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.biometric_setup_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.biometric_setup_body), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        if (available) {
            Button(onClick = {
                val activity = context as? FragmentActivity
                if (activity != null) {
                    BiometricHelper.authenticate(activity, "OTP Vault", "Confirm to enable biometrics",
                        onSuccess = onEnable, onError = { onSkip() })
                } else onSkip()
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.enable_biometric)) }
        }
        TextButton(onClick = onSkip) { Text(stringResource(R.string.skip)) }
    }
}
