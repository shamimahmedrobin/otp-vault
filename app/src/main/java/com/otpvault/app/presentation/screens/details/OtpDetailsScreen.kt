package com.otpvault.app.presentation.screens.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.otpvault.app.R
import com.otpvault.app.core.util.PhoneMask
import com.otpvault.app.core.util.TimeFormat
import com.otpvault.app.presentation.components.CountdownText
import com.otpvault.app.presentation.screens.home.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun OtpDetailsScreen(
    otpId: String,
    onDeleted: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val item = state.items.firstOrNull { it.id == otpId }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var revealed by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.otp_details), style = MaterialTheme.typography.headlineMedium)
        if (item == null) {
            Text(stringResource(R.string.no_results))
            return@Column
        }
        Text(item.service.name, style = MaterialTheme.typography.titleLarge)
        if (!item.phone.isNullOrBlank()) Text(PhoneMask.mask(item.phone))
        Text(
            if (revealed) item.code else "•".repeat(item.code.length.coerceIn(4, 10)),
            style = MaterialTheme.typography.displaySmall
        )
        CountdownText(item.expiresAt)
        Text(stringResource(R.string.created_at) + ": " + TimeFormat.dateTime(item.createdAt, Locale.getDefault()))
        Text(stringResource(R.string.expires_at) + ": " + TimeFormat.dateTime(item.expiresAt, Locale.getDefault()))
        if (!item.note.isNullOrBlank()) Text(item.note)
        Text(stringResource(if (item.isUsed) R.string.used else R.string.active))

        Button(onClick = { revealed = !revealed }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (revealed) R.string.hide else R.string.show))
        }
        OutlinedButton(onClick = {
            clipboard.setText(AnnotatedString(item.code))
            android.widget.Toast.makeText(context, context.getString(R.string.copied), android.widget.Toast.LENGTH_SHORT).show()
            CoroutineScope(Dispatchers.Main).launch { delay(30_000); clipboard.setText(AnnotatedString("")) }
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.copy)) }
        OutlinedButton(onClick = { viewModel.markUsed(item.id) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.mark_used))
        }
        OutlinedButton(onClick = { onEdit(item.id) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.edit_otp))
        }
        OutlinedButton(onClick = { viewModel.delete(item.id); onDeleted() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.delete))
        }
    }
}
