package com.example.restredder.ui.store

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.restredder.R
import com.example.restredder.data.model.PlaceResult
import com.example.restredder.data.model.StoreCategory
import com.example.restredder.data.photo.PhotoEncoder
import com.example.restredder.data.photo.PhotoTooLargeException
import com.example.restredder.data.repository.StoreRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateStoreUiState(
    val name: String = "",
    val category: StoreCategory? = null,
    val location: PlaceResult? = null,
    val photoBase64: String = "",
    val isProcessingPhoto: Boolean = false,
    @StringRes val photoErrorRes: Int? = null,
    // True when a photo permission can only be turned on in the app settings.
    val showOpenSettings: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
) {
    val canSave: Boolean
        get() = name.isNotBlank() && category != null && location != null &&
                !isLoading && !isProcessingPhoto
}

// AndroidViewModel: reading the photo needs the app context.
class CreateStoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StoreRepository()
    private val photoEncoder = PhotoEncoder(application)

    // The form lives here, so it survives going to the location picker and back.
    private val _uiState = MutableStateFlow(CreateStoreUiState())
    val uiState = _uiState.asStateFlow()

    private var photoJob: Job? = null

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, errorMessageRes = null) }
    }

    fun onCategorySelected(category: StoreCategory) {
        _uiState.update { it.copy(category = category, errorMessageRes = null) }
    }

    fun onLocationPicked(location: PlaceResult) {
        _uiState.update { it.copy(location = location, errorMessageRes = null) }
    }

    fun createCameraUri(): Uri = photoEncoder.createCameraUri()

    fun onPhotoPicked(uri: Uri) {
        photoJob?.cancel()
        photoJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isProcessingPhoto = true, photoErrorRes = null, showOpenSettings = false)
            }

            photoEncoder.encode(uri).fold(
                onSuccess = { base64 ->
                    _uiState.update { it.copy(photoBase64 = base64, isProcessingPhoto = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isProcessingPhoto = false,
                            photoErrorRes = if (error is PhotoTooLargeException) {
                                R.string.error_photo_too_large
                            } else {
                                R.string.error_photo_failed
                            }
                        )
                    }
                }
            )
        }
    }

    fun onPhotoPermissionDenied(isCamera: Boolean) {
        _uiState.update {
            it.copy(
                photoErrorRes = if (isCamera) {
                    R.string.error_camera_permission_denied
                } else {
                    R.string.error_gallery_permission_denied
                },
                showOpenSettings = false
            )
        }
    }

    fun onPhotoPermissionBlocked(isCamera: Boolean) {
        _uiState.update {
            it.copy(
                photoErrorRes = if (isCamera) {
                    R.string.camera_permission_settings
                } else {
                    R.string.gallery_permission_settings
                },
                showOpenSettings = true
            )
        }
    }

    fun onCameraUnavailable() {
        _uiState.update { it.copy(photoErrorRes = R.string.error_no_camera, showOpenSettings = false) }
    }

    fun removePhoto() {
        photoJob?.cancel()
        _uiState.update {
            it.copy(
                photoBase64 = "",
                isProcessingPhoto = false,
                photoErrorRes = null,
                showOpenSettings = false
            )
        }
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
                address = location.address,
                photoBase64 = state.photoBase64
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
