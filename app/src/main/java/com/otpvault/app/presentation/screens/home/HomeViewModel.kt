package com.otpvault.app.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otpvault.app.domain.model.OtpItem
import com.otpvault.app.domain.model.Service
import com.otpvault.app.domain.repository.OtpRepository
import com.otpvault.app.domain.repository.ServiceRepository
import com.otpvault.app.domain.usecase.CleanupExpiredUseCase
import com.otpvault.app.domain.usecase.SeedServicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val items: List<OtpItem> = emptyList(),
    val services: List<Service> = emptyList(),
    val activeCount: Int = 0,
    val expiringSoonCount: Int = 0,
    val serviceCount: Int = 0,
    val query: String = "",
    val selectedServiceId: String? = null
) {
    val filtered: List<OtpItem>
        get() = items.filter { item ->
            (selectedServiceId == null || item.service.id == selectedServiceId) &&
                (query.isBlank() || item.service.name.contains(query, ignoreCase = true) ||
                    (item.note?.contains(query, ignoreCase = true) == true))
        }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val otpRepository: OtpRepository,
    serviceRepository: ServiceRepository,
    seedServices: SeedServicesUseCase,
    cleanupExpired: CleanupExpiredUseCase
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedService = MutableStateFlow<String?>(null)

    val state: StateFlow<HomeUiState> = combine(
        otpRepository.observeAll(),
        serviceRepository.observeAll(),
        otpRepository.observeActiveCount(),
        otpRepository.observeExpiringSoonCount(),
        serviceRepository.observeCount()
    ) { items, services, active, soon, count ->
        HomeUiState(items, services, active, soon, count)
    }.combine(query) { s, q -> s.copy(query = q) }
        .combine(selectedService) { s, sel -> s.copy(selectedServiceId = sel) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            seedServices()
            cleanupExpired() // startup cleanup check, in addition to WorkManager
        }
    }

    fun setQuery(q: String) { query.value = q }
    fun selectService(id: String?) { selectedService.value = id }
    fun delete(id: String) { viewModelScope.launch { otpRepository.delete(id) } }
    fun markUsed(id: String) { viewModelScope.launch { otpRepository.markUsed(id) } }
}
