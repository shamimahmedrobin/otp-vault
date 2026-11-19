package com.otpvault.app.presentation.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.otpvault.app.R
import com.otpvault.app.domain.model.OtpStatus
import com.otpvault.app.presentation.components.EmptyState
import com.otpvault.app.presentation.components.OtpCard
import com.otpvault.app.presentation.screens.home.HomeViewModel

@Composable
fun HistoryScreen(
    onOpenDetails: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var filter by remember { mutableStateOf<OtpStatus?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val revealed = remember { mutableStateMapOf<String, Boolean>() }

    // History only ever shows not-yet-expired rows: expired rows are hard-deleted
    // by cleanup, so nothing stale can accumulate here.
    val now = System.currentTimeMillis()
    val rows = state.items.filter { item ->
        val st = item.status(now)
        (filter == null || st == filter) &&
            (state.query.isBlank() || item.service.name.contains(state.query, ignoreCase = true))
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.history), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { confirmClear = true }) { Text(stringResource(R.string.clear_all)) }
        }
        OutlinedTextField(
            value = state.query, onValueChange = viewModel::setQuery,
            label = { Text(stringResource(R.string.search_hint)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text(stringResource(R.string.all)) })
            FilterChip(selected = filter == OtpStatus.ACTIVE, onClick = { filter = OtpStatus.ACTIVE }, label = { Text(stringResource(R.string.active)) })
            FilterChip(selected = filter == OtpStatus.USED, onClick = { filter = OtpStatus.USED }, label = { Text(stringResource(R.string.used)) })
            FilterChip(selected = filter == OtpStatus.EXPIRED, onClick = { filter = OtpStatus.EXPIRED }, label = { Text(stringResource(R.string.expired)) })
        }
        if (rows.isEmpty()) {
            EmptyState(stringResource(R.string.history), stringResource(R.string.history_empty))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rows, key = { it.id }) { item ->
                    OtpCard(item, revealed[item.id] == true,
                        onToggleReveal = { revealed[item.id] = !(revealed[item.id] ?: false) },
                        onCopy = { clipboard.setText(AnnotatedString(item.code)) },
                        onDelete = { viewModel.delete(item.id) },
                        onClick = { onOpenDetails(item.id) })
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_all)) },
            text = { Text(stringResource(R.string.confirm_clear)) },
            confirmButton = {
                TextButton(onClick = {
                    state.items.forEach { viewModel.delete(it.id) }
                    confirmClear = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
