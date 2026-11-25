package com.example.restredder.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restredder.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val email: String = "",
    val role: String? = null
) {
    val isMerchant: Boolean get() = role == "merchant"
}

class HomeViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = repository.getCurrentUser()
            _uiState.value = HomeUiState(
                isLoading = false,
                name = user?.name.orEmpty(),
                email = user?.email.orEmpty(),
                role = user?.role
            )
        }
    }

    fun logout() = repository.logout()
}
