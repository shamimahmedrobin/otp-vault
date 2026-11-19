package com.otpvault.app.presentation.screens.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.otpvault.app.R
import com.otpvault.app.presentation.components.serviceIcon

val AVAILABLE_ICONS = listOf("apps", "phone_android", "account_balance", "account_balance_wallet",
    "payments", "rocket_launch", "touch_app", "mail", "share", "shopping_bag")

@Composable
fun ServicesScreen(viewModel: ServicesViewModel = hiltViewModel()) {
    val services by viewModel.services.collectAsState()
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var icon by remember { mutableStateOf("apps") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.services), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            Button(onClick = { showAdd = true }) { Text(stringResource(R.string.add_service)) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(services, key = { it.id }) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(serviceIcon(s.icon), contentDescription = s.name)
                        Column(Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.titleMedium)
                            Text(s.category + if (s.isCustom) " • " + stringResource(R.string.custom_service) else "",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        if (s.isCustom) {
                            IconButton(onClick = {
                                viewModel.delete(s.id) { msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete)) }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.add_service)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.service_name)) })
                    OutlinedTextField(category, { category = it }, label = { Text(stringResource(R.string.category)) })
                    OutlinedTextField(icon, { icon = it }, label = { Text(stringResource(R.string.icon) + ": " + AVAILABLE_ICONS.joinToString()) })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.add(name, category.ifBlank { "Other" }, icon.ifBlank { "apps" }) { msg ->
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                        }
                        name = ""; showAdd = false
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
