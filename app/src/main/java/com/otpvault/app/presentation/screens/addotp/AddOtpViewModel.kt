package com.otpvault.app.presentation.screens.addotp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otpvault.app.core.util.OtpValidator
import com.otpvault.app.core.util.PhoneMask
import com.otpvault.app.domain.model.ExpiryOption
import com.otpvault.app.domain.model.Service
import com.otpvault.app.domain.repository.OtpRepository
import com.otpvault.app.domain.repository.ServiceRepository
import com.otpvault.app.domain.usecase.AddOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddOtpUiState(
    val serviceId: String? = null,
    val phone: String = "",
    val code: String = "",
    val note: String = "",
    val expiry: ExpiryOption = ExpiryOption.FIVE,
    val customMinutes: String = "",
    val useCustom: Boolean = false,
    val error: String? = null,
    val duplicateWarning: Boolean = false,
    val saved: Boolean = false,
    val editingId: String? = null
)

@HiltViewModel
class AddOtpViewModel @Inject constructor(
    private val otpRepository: OtpRepository,
    serviceRepository: ServiceRepository,
    private val addOtp: AddOtpUseCase
) : ViewModel() {

    val services: StateFlow<List<Service>> = serviceRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow(AddOtpUiState())
    val state: StateFlow<AddOtpUiState> = _state.asStateFlow()

    fun update(transform: (AddOtpUiState) -> AddOtpUiState) {
        _state.value = transform(_state.value)
        val s = _state.value
        if (s.serviceId != null && s.code.isNotBlank()) {
            viewModelScope.launch {
                val dup = otpRepository.hasDuplicate(s.serviceId, s.code)
                _state.value = _state.value.copy(duplicateWarning = dup && s.editingId == null)
            }
        }
    }

    fun loadForEdit(id: String) {
        viewModelScope.launch {
            val item = otpRepository.observeById(id).first() ?: return@launch
            _state.value = AddOtpUiState(
                serviceId = item.service.id, phone = item.phone ?: "",
                code = item.code, note = item.note ?: "",
                editingId = item.id
            )
        }
    }

    fun save() {
        val s = _state.value
        val serviceId = s.serviceId ?: run {
            _state.value = s.copy(error = "service"); return
        }
        if (!OtpValidator.isValid(s.code)) {
            _state.value = s.copy(error = "otp"); return
        }
        if (!PhoneMask.isValid(s.phone)) {
            _state.value = s.copy(error = "phone"); return
        }
        viewModelScope.launch {
            if (s.editingId != null) {
                val current = otpRepository.observeById(s.editingId).first()
                current?.let {
                    val minutes = if (s.useCustom) s.customMinutes.toIntOrNull() ?: 5 else s.expiry.minutes
                    otpRepository.update(it, s.code, s.phone.ifBlank { null }, s.note.ifBlank { null },
                        System.currentTimeMillis() + minutes * 60_000L)
                }
            } else {
                addOtp(
                    serviceId = serviceId,
                    phone = s.phone.ifBlank { null },
                    code = s.code,
                    note = s.note.ifBlank { null },
                    expiry = s.expiry,
                    customMinutes = if (s.useCustom) s.customMinutes.toIntOrNull() else null
                )
            }
            // Clear sensitive form values after save.
            _state.value = AddOtpUiState(saved = true)
        }
    }
}
