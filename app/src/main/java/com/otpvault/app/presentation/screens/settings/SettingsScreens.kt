package com.otpvault.app.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.otpvault.app.BuildConfig
import com.otpvault.app.R
import com.otpvault.app.core.preferences.ThemeMode
import com.otpvault.app.domain.model.ExpiryOption

@Composable
fun SettingsScreen(
    onOpenSecurity: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenServices: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var confirmWipe by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)

        SectionTitle(stringResource(R.string.settings_security))
        NavRow(stringResource(R.string.settings_security)) { onOpenSecurity() }

        SectionTitle(stringResource(R.string.settings_otp))
        RowItem(stringResource(R.string.auto_delete), state.autoDelete) { viewModel.setAutoDelete(it) }
        RowItem(stringResource(R.string.clipboard_clear), state.clipboardClear) { viewModel.setClipboardClear(it) }
        RowItem(stringResource(R.string.confirm_before_delete), state.confirmDelete) { viewModel.setConfirmDelete(it) }
        Text(stringResource(R.string.default_expiry) + ": " + stringResource(ExpiryOption.fromMinutes(state.defaultExpiryMinutes).let {
            when (it) { ExpiryOption.FIVE -> R.string.five_min; ExpiryOption.FIFTEEN -> R.string.fifteen_min
                ExpiryOption.THIRTY -> R.string.thirty_min; ExpiryOption.SIXTY -> R.string.one_hour; ExpiryOption.DAY -> R.string.one_day }
        }), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpiryOption.entries.forEach { opt ->
                TextButton(onClick = { viewModel.setDefaultExpiry(opt.minutes) }) {
                    Text(stringResource(R.string.minutes_value, opt.minutes))
                }
            }
        }
        NavRow(stringResource(R.string.services)) { onOpenServices() }

        SectionTitle(stringResource(R.string.settings_appearance))
        ThemeMode.entries.forEach { mode ->
            Row(Modifier.fillMaxWidth().clickable { viewModel.setTheme(mode) }.padding(vertical = 6.dp)) {
                RadioButton(selected = state.theme == mode, onClick = { viewModel.setTheme(mode) })
                Text(stringResource(when (mode) {
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                    ThemeMode.SYSTEM -> R.string.theme_system
                }))
            }
        }
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "System", "en" to "English", "bn" to "বাংলা").forEach { (code, label) ->
                TextButton(onClick = { viewModel.setLanguage(code) }) {
                    Text((if (state.language == code) "● " else "") + label)
                }
            }
        }

        SectionTitle(stringResource(R.string.settings_data))
        NavRow(stringResource(R.string.backup)) { onOpenBackup() }
        NavRow(stringResource(R.string.restore)) { onOpenBackup() }
        NavRow(stringResource(R.string.delete_all_data)) { confirmWipe = true }

        SectionTitle(stringResource(R.string.settings_about))
        NavRow(stringResource(R.string.about)) { onOpenAbout() }
        NavRow(stringResource(R.string.privacy_policy)) { onOpenPrivacy() }
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text(stringResource(R.string.delete_all_data)) },
            text = { Text(stringResource(R.string.confirm_clear)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmWipe = false
                    viewModel.deleteAllData { }
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
fun SecuritySettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var showChangePin by remember { mutableStateOf(false) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(stringResource(R.string.settings_security), style = MaterialTheme.typography.headlineMedium)
        RowItem(stringResource(R.string.biometric_unlock), state.biometric) { viewModel.setBiometric(it) }
        RowItem(stringResource(R.string.lock_on_background), state.lockOnBackground) { viewModel.setLockOnBg(it) }
        Text(stringResource(R.string.auto_lock) + ": ${state.autoLockSeconds}s", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(30, 60, 300, 900).forEach { sec ->
                TextButton(onClick = { viewModel.setAutoLock(sec) }) { Text("${sec}s") }
            }
        }
        NavRow(stringResource(R.string.change_pin)) { showChangePin = true }
    }

    if (showChangePin) {
        AlertDialog(
            onDismissRequest = { showChangePin = false },
            title = { Text(stringResource(R.string.change_pin)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(currentPin, { currentPin = it.take(6) },
                        label = { Text(stringResource(R.string.enter_current_pin)) })
                    androidx.compose.material3.OutlinedTextField(newPin, { newPin = it.take(6) },
                        label = { Text(stringResource(R.string.enter_new_pin)) })
                    if (pinError != null) Text(pinError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        !viewModel.verifyPin(currentPin) -> pinError = "Incorrect PIN"
                        newPin.length != 6 || !newPin.all { it.isDigit() } -> pinError = "New PIN must be 6 digits"
                        else -> { viewModel.changePin(newPin); showChangePin = false; currentPin = ""; newPin = "" }
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showChangePin = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
}

@Composable
private fun RowItem(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    Text(label, style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp))
}

@Composable
fun AboutScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.about), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.app_name) + " — " + stringResource(R.string.tagline))
        Text(stringResource(R.string.version) + ": " + BuildConfig.VERSION_NAME)
        Text(stringResource(R.string.contact) + ": solution.home.bd@gmail.com")
        Text(stringResource(R.string.licenses) + ": AndroidX, Jetpack Compose, Room, WorkManager, Hilt (Apache 2.0)")
    }
}

@Composable
fun PrivacyPolicyScreen() {
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(R.string.privacy_policy), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodyLarge)
    }
}
