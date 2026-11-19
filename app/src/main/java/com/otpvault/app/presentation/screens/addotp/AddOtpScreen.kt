package com.otpvault.app.presentation.screens.addotp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.otpvault.app.R
import com.otpvault.app.domain.model.ExpiryOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOtpScreen(
    editId: String? = null,
    onSaved: () -> Unit,
    viewModel: AddOtpViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val services by viewModel.services.collectAsState()
    var serviceMenuOpen by remember { mutableStateOf(false) }
    var serviceQuery by remember { mutableStateOf("") }

    LaunchedEffect(editId) { if (editId != null) viewModel.loadForEdit(editId) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    val selectedService = services.firstOrNull { it.id == state.serviceId }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(if (editId != null) R.string.edit_otp else R.string.add_otp),
            style = MaterialTheme.typography.headlineMedium)

        ExposedDropdownMenuBox(expanded = serviceMenuOpen, onExpandedChange = { serviceMenuOpen = it }) {
            OutlinedTextField(
                value = serviceQuery.ifBlank { selectedService?.name ?: "" },
                onValueChange = { serviceQuery = it; serviceMenuOpen = true },
                label = { Text(stringResource(R.string.service)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceMenuOpen) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = serviceMenuOpen, onDismissRequest = { serviceMenuOpen = false }) {
                services.filter { it.name.contains(serviceQuery, ignoreCase = true) }.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.name) },
                        onClick = {
                            viewModel.update { it.copy(serviceId = s.id) }
                            serviceQuery = ""; serviceMenuOpen = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.phone,
            onValueChange = { v -> viewModel.update { it.copy(phone = v) } },
            label = { Text(stringResource(R.string.phone_number)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = state.error == "phone",
            supportingText = { if (state.error == "phone") Text(stringResource(R.string.invalid_phone)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )

        OutlinedTextField(
            value = state.code,
            onValueChange = { v -> viewModel.update { it.copy(code = v.take(10)) } },
            label = { Text(stringResource(R.string.otp_code)) },
            isError = state.error == "otp",
            supportingText = { if (state.error == "otp") Text(stringResource(R.string.invalid_otp)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )

        if (state.duplicateWarning) {
            Text(stringResource(R.string.duplicate_warning), color = MaterialTheme.colorScheme.error)
        }

        OutlinedTextField(
            value = state.note,
            onValueChange = { v -> viewModel.update { it.copy(note = v) } },
            label = { Text(stringResource(R.string.note_optional)) },
            modifier = Modifier.fillMaxWidth()
        )

        Text(stringResource(R.string.expiry_duration), style = MaterialTheme.typography.titleMedium)
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ExpiryOption.FIVE to R.string.five_min,
                ExpiryOption.FIFTEEN to R.string.fifteen_min,
                ExpiryOption.THIRTY to R.string.thirty_min,
                ExpiryOption.SIXTY to R.string.one_hour,
                ExpiryOption.DAY to R.string.one_day
            ).forEach { (opt, label) ->
                FilterChip(
                    selected = !state.useCustom && state.expiry == opt,
                    onClick = { viewModel.update { it.copy(expiry = opt, useCustom = false) } },
                    label = { Text(stringResource(label)) }
                )
            }
            FilterChip(
                selected = state.useCustom,
                onClick = { viewModel.update { it.copy(useCustom = true) } },
                label = { Text(stringResource(R.string.custom_duration)) }
            )
        }
        if (state.useCustom) {
            OutlinedTextField(
                value = state.customMinutes,
                onValueChange = { v -> viewModel.update { it.copy(customMinutes = v.filter { c -> c.isDigit() }) } },
                label = { Text(stringResource(R.string.custom_duration)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        }

        Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save_otp))
        }
    }
}
