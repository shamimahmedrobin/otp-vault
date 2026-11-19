package com.otpvault.app.presentation.screens.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otpvault.app.domain.model.Service
import com.otpvault.app.domain.repository.ServiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val repo: ServiceRepository
) : ViewModel() {
    val services: StateFlow<List<Service>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(name: String, category: String, icon: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try { repo.addCustom(name, category, icon) } catch (e: Exception) { onError(e.message ?: "Error") }
        }
    }

    fun update(service: Service, onError: (String) -> Unit) {
        viewModelScope.launch {
            try { repo.updateCustom(service) } catch (e: Exception) { onError(e.message ?: "Error") }
        }
    }

    fun delete(id: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try { repo.deleteCustom(id) } catch (e: Exception) { onError(e.message ?: "Error") }
        }
    }
}
