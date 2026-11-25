package com.example.restredder.ui.store

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restredder.R
import com.example.restredder.data.model.Store
import com.example.restredder.data.repository.StoreRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MyStoresUiState(
    val isLoading: Boolean = true,
    val stores: List<Store> = emptyList(),
    @StringRes val errorMessageRes: Int? = null
)

class MyStoresViewModel(
    repository: StoreRepository = StoreRepository()
) : ViewModel() {

    val uiState: StateFlow<MyStoresUiState> = repository.observeMyStores()
        .map { stores -> MyStoresUiState(isLoading = false, stores = stores) }
        .catch { error ->
            emit(MyStoresUiState(isLoading = false, errorMessageRes = error.toMessageRes()))
        }
        .stateIn(
            scope = viewModelScope,
            // Keep listening for 5 s after the screen goes away (e.g. while rotating).
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyStoresUiState()
        )

    @StringRes
    private fun Throwable.toMessageRes(): Int =
        if (this is FirebaseFirestoreException &&
            code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        ) {
            R.string.error_stores_permission
        } else {
            R.string.error_stores_load_failed
        }
}
