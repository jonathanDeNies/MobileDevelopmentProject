package com.example.restredder.ui.store

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restredder.R
import com.example.restredder.data.model.PlaceResult
import com.example.restredder.data.model.Store
import com.example.restredder.data.repository.StoreRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyStoresUiState(
    val isLoading: Boolean = true,
    val stores: List<Store> = emptyList(),
    @StringRes val errorMessageRes: Int? = null,
    // Store waiting for the user to confirm deletion (shows the dialog).
    val storeToDelete: Store? = null,
    val isDeleting: Boolean = false,
    @StringRes val deleteErrorRes: Int? = null,
    val isUpdatingLocation: Boolean = false,
    // Shown once in a snackbar, then cleared with onMessageShown().
    @StringRes val messageRes: Int? = null
)

private data class DeleteState(
    val storeToDelete: Store? = null,
    val isDeleting: Boolean = false,
    @StringRes val deleteErrorRes: Int? = null
)

private data class LocationUpdateState(
    val isUpdating: Boolean = false,
    @StringRes val messageRes: Int? = null
)

class MyStoresViewModel(
    private val repository: StoreRepository = StoreRepository()
) : ViewModel() {

    private val deleteState = MutableStateFlow(DeleteState())
    private val locationState = MutableStateFlow(LocationUpdateState())

    // The store whose location is being changed in the location picker.
    private var storeBeingMovedId: String? = null

    private val storesState = repository.observeMyStores()
        .map { stores -> MyStoresUiState(isLoading = false, stores = stores) }
        .catch { error ->
            emit(MyStoresUiState(isLoading = false, errorMessageRes = error.toLoadMessageRes()))
        }

    val uiState: StateFlow<MyStoresUiState> = combine(
        storesState,
        deleteState,
        locationState
    ) { stores, delete, location ->
        stores.copy(
            storeToDelete = delete.storeToDelete,
            isDeleting = delete.isDeleting,
            deleteErrorRes = delete.deleteErrorRes,
            isUpdatingLocation = location.isUpdating,
            messageRes = location.messageRes
        )
    }.stateIn(
        scope = viewModelScope,
        // Keep listening for 5 s after the screen goes away (e.g. while rotating).
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MyStoresUiState()
    )

    fun onDeleteClick(store: Store) {
        deleteState.value = DeleteState(storeToDelete = store)
    }

    fun onDeleteDismiss() {
        if (deleteState.value.isDeleting) return
        deleteState.value = DeleteState()
    }

    fun confirmDelete() {
        val store = deleteState.value.storeToDelete ?: return
        if (deleteState.value.isDeleting) return

        viewModelScope.launch {
            deleteState.update { it.copy(isDeleting = true, deleteErrorRes = null) }

            repository.deleteStore(store.id).fold(
                // The live list removes the store by itself.
                onSuccess = { deleteState.value = DeleteState() },
                onFailure = { error ->
                    deleteState.update {
                        it.copy(isDeleting = false, deleteErrorRes = error.toDeleteMessageRes())
                    }
                }
            )
        }
    }

    fun onChangeLocationClick(store: Store) {
        storeBeingMovedId = store.id
    }

    /** Called with the location chosen in the location picker. */
    fun onLocationPicked(place: PlaceResult) {
        val storeId = storeBeingMovedId ?: return
        storeBeingMovedId = null

        viewModelScope.launch {
            locationState.value = LocationUpdateState(isUpdating = true)

            val result = repository.updateStoreLocation(
                storeId = storeId,
                latitude = place.latitude,
                longitude = place.longitude,
                address = place.address
            )

            // The live list shows the new address by itself.
            locationState.value = LocationUpdateState(
                messageRes = result.fold(
                    onSuccess = { R.string.location_updated },
                    onFailure = { it.toLocationMessageRes() }
                )
            )
        }
    }

    fun onMessageShown() {
        locationState.update { it.copy(messageRes = null) }
    }

    @StringRes
    private fun Throwable.toLocationMessageRes(): Int = when {
        this is FirebaseNetworkException -> R.string.error_network
        this is FirebaseFirestoreException &&
                code == FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_network
        this is FirebaseFirestoreException &&
                code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            R.string.error_update_location_permission
        else -> R.string.error_update_location_failed
    }

    @StringRes
    private fun Throwable.toLoadMessageRes(): Int =
        if (this is FirebaseFirestoreException &&
            code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        ) {
            R.string.error_stores_permission
        } else {
            R.string.error_stores_load_failed
        }

    @StringRes
    private fun Throwable.toDeleteMessageRes(): Int = when {
        this is FirebaseNetworkException -> R.string.error_network
        this is FirebaseFirestoreException &&
                code == FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_network
        this is FirebaseFirestoreException &&
                code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            R.string.error_delete_store_permission
        else -> R.string.error_delete_store_failed
    }
}
