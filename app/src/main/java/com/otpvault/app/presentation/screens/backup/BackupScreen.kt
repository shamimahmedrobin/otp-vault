package com.otpvault.app.presentation.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.otpvault.app.R
import com.otpvault.app.data.backup.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(val backup: BackupManager) : ViewModel()

@Composable
fun BackupRestoreScreen(viewModel: BackupViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        if (uri != null && password.length >= 8) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val json = viewModel.backup.exportJson()
                        val encrypted = viewModel.backup.encryptBackup(json.toByteArray(Charsets.UTF_8), password.toCharArray())
                        context.contentResolver.openOutputStream(uri)?.use { it.write(encrypted) }
                        true
                    } catch (e: Exception) { false }
                }
                status = context.getString(if (ok) R.string.backup_created else R.string.backup_failed)
                password = ""
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null && password.isNotEmpty()) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext false
                        val plain = viewModel.backup.decryptBackup(bytes, password.toCharArray()) ?: return@withContext false
                        viewModel.backup.importJson(String(plain, Charsets.UTF_8))
                        true
                    } catch (e: Exception) { false }
                }
                status = context.getString(if (ok) R.string.backup_restored else R.string.backup_failed)
                password = ""
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.backup), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.backup_explain), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text(stringResource(R.string.backup_password) + " (min 8)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Button(
            onClick = { createLauncher.launch("otp-vault-backup.otpvault") },
            enabled = password.length >= 8,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.backup)) }
        OutlinedButton(
            onClick = { openLauncher.launch(arrayOf("*/*")) },
            enabled = password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.restore)) }
        if (status != null) Text(status!!)
    }
}
