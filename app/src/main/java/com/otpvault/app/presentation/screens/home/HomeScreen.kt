package com.otpvault.app.presentation.screens.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.otpvault.app.R
import com.otpvault.app.presentation.components.EmptyState
import com.otpvault.app.presentation.components.OtpCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOpenDetails: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val revealed = remember { mutableStateMapOf<String, Boolean>() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.greeting) + " • " + stringResource(R.string.security_on),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings)) }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(stringResource(R.string.active_otps), state.activeCount.toString(), Modifier.weight(1f))
            SummaryCard(stringResource(R.string.expiring_soon), state.expiringSoonCount.toString(), Modifier.weight(1f))
            SummaryCard(stringResource(R.string.total_services), state.serviceCount.toString(), Modifier.weight(1f))
        }

        OutlinedTextField(
            value = state.query, onValueChange = viewModel::setQuery,
            label = { Text(stringResource(R.string.search)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.selectedServiceId == null, onClick = { viewModel.selectService(null) },
                label = { Text(stringResource(R.string.all)) })
            state.services.forEach { s ->
                FilterChip(selected = state.selectedServiceId == s.id, onClick = { viewModel.selectService(s.id) },
                    label = { Text(s.name) })
            }
        }

        Text(stringResource(R.string.recent_otps), style = MaterialTheme.typography.titleLarge)

        if (state.filtered.isEmpty()) {
            EmptyState(stringResource(R.string.no_otps_title), stringResource(R.string.no_otps_body))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.filtered, key = { it.id }) { item ->
                    OtpCard(
                        item = item,
                        revealed = revealed[item.id] == true,
                        onToggleReveal = { revealed[item.id] = !(revealed[item.id] ?: false) },
                        onCopy = {
                            clipboard.setText(AnnotatedString(item.code))
                            android.widget.Toast.makeText(context, context.getString(R.string.copied), android.widget.Toast.LENGTH_SHORT).show()
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(30_000)
                                clipboard.setText(AnnotatedString(""))
                            }
                        },
                        onDelete = { viewModel.delete(item.id) },
                        onClick = { onOpenDetails(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
