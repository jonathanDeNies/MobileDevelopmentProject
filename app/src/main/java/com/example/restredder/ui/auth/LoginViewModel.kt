package com.example.restredder.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restredder.R
import com.example.restredder.data.repository.AuthError
import com.example.restredder.data.repository.AuthException
import com.example.restredder.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
)

class LoginViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)

            _uiState.value = repository.login(email, password).fold(
                onSuccess = { LoginUiState(isSuccess = true) },
                onFailure = { LoginUiState(errorMessageRes = it.toMessageRes()) }
            )
        }
    }

    @StringRes
    private fun Throwable.toMessageRes(): Int =
        when ((this as? AuthException)?.error) {
            AuthError.INVALID_CREDENTIALS -> R.string.error_invalid_credentials
            AuthError.NETWORK -> R.string.error_network
            AuthError.TOO_MANY_REQUESTS -> R.string.error_too_many_requests
            else -> R.string.error_login_failed
        }
}
