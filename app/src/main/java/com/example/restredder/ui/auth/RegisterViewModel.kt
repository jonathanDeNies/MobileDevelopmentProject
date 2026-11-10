package com.example.restredder.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restredder.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class RegisterViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun register(
        name: String,
        email: String,
        password: String,
        role: String
    ) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = RegisterUiState(
                isLoading = true
            )

            val result = repository.register(
                name, email, password, role
            )

            _uiState.value = if (result.isSuccess) {
                RegisterUiState(isSuccess = true)
            } else {
                RegisterUiState(
                    errorMessage = result.exceptionOrNull()
                        ?.localizedMessage
                        ?: "Registration failed"
                )
            }
        }
    }
}
