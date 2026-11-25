package com.example.restredder.ui.store

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restredder.R
import com.example.restredder.data.model.PlaceResult
import com.example.restredder.data.model.StoreCategory
import com.example.restredder.data.repository.StoreRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateStoreUiState(
    val name: String = "",
    val category: StoreCategory? = null,
    val location: PlaceResult? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
) {
    val canSave: Boolean
        get() = name.isNotBlank() && category != null && location != null && !isLoading
}

class CreateStoreViewModel(
    private val repository: StoreRepository = StoreRepository()
) : ViewModel() {

    // The form lives here, so it survives going to the location picker and back.
    private val _uiState = MutableStateFlow(CreateStoreUiState())
    val uiState = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, errorMessageRes = null) }
    }

    fun onCategorySelected(category: StoreCategory) {
        _uiState.update { it.copy(category = category, errorMessageRes = null) }
    }

    fun onLocationPicked(location: PlaceResult) {
        _uiState.update { it.copy(location = location, errorMessageRes = null) }
    }

    fun createStore() {
        val state = _uiState.value
        val category = state.category ?: return
        val location = state.location ?: return
        if (!state.canSave) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }

            val result = repository.createStore(
                name = state.name,
                category = category,
                latitude = location.latitude,
                longitude = location.longitude,
                address = location.address
            )

            _uiState.update {
                result.fold(
                    onSuccess = { _ -> it.copy(isLoading = false, isSuccess = true) },
                    onFailure = { error ->
                        it.copy(isLoading = false, errorMessageRes = error.toMessageRes())
                    }
                )
            }
        }
    }

    @StringRes
    private fun Throwable.toMessageRes(): Int = when {
        this is FirebaseNetworkException -> R.string.error_network
        this is FirebaseFirestoreException &&
                code == FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_network
        this is FirebaseFirestoreException &&
                code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_store_permission
        else -> R.string.error_store_save_failed
    }
}
