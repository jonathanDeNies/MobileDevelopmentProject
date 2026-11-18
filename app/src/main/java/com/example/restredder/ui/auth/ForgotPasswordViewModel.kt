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

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val isEmailSent: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
)

class ForgotPasswordViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun sendResetLink(email: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = ForgotPasswordUiState(isLoading = true)

            _uiState.value = repository.sendPasswordReset(email).fold(
                onSuccess = { ForgotPasswordUiState(isEmailSent = true) },
                onFailure = { ForgotPasswordUiState(errorMessageRes = it.toMessageRes()) }
            )
        }
    }

    // A badly formatted email comes back from Firebase as "invalid credentials".
    @StringRes
    private fun Throwable.toMessageRes(): Int =
        when ((this as? AuthException)?.error) {
            AuthError.INVALID_CREDENTIALS -> R.string.error_invalid_email
            AuthError.NETWORK -> R.string.error_network
            AuthError.TOO_MANY_REQUESTS -> R.string.error_too_many_requests
            else -> R.string.error_reset_failed
        }
}
