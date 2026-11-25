package com.example.restredder.ui.location

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restredder.R
import com.example.restredder.data.model.PlaceResult
import com.example.restredder.data.repository.GeocodingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class LocationPickerUiState(
    val query: String = "",
    val results: List<PlaceResult> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val selected: PlaceResult? = null,
    val isLoadingAddress: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
)

class LocationPickerViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repository = GeocodingRepository()

    private val _uiState = MutableStateFlow(LocationPickerUiState())
    val uiState = _uiState.asStateFlow()

    private var addressJob: Job? = null

    init {
        // Start from the location that was already chosen (navigation arguments).
        val latitude = savedStateHandle.get<String>(ARG_LATITUDE)?.toDoubleOrNull()
        val longitude = savedStateHandle.get<String>(ARG_LONGITUDE)?.toDoubleOrNull()
        val address = savedStateHandle.get<String>(ARG_ADDRESS).orEmpty()
        if (latitude != null && longitude != null) {
            _uiState.update { it.copy(selected = PlaceResult(latitude, longitude, address)) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, errorMessageRes = null) }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(query = "", results = emptyList(), hasSearched = false, errorMessageRes = null)
        }
    }

    fun search() {
        val query = _uiState.value.query
        if (query.isBlank() || _uiState.value.isSearching) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessageRes = null) }

            repository.search(query).fold(
                onSuccess = { results ->
                    _uiState.update {
                        it.copy(isSearching = false, hasSearched = true, results = results)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            hasSearched = true,
                            results = emptyList(),
                            errorMessageRes = error.toMessageRes()
                        )
                    }
                }
            )
        }
    }

    fun onResultSelected(place: PlaceResult) {
        addressJob?.cancel()
        _uiState.update {
            it.copy(
                selected = place,
                isLoadingAddress = false,
                results = emptyList(),
                hasSearched = false,
                errorMessageRes = null
            )
        }
    }

    fun onMapTapped(latitude: Double, longitude: Double) {
        val place = PlaceResult(latitude, longitude, address = "")
        _uiState.update {
            it.copy(
                selected = place,
                isLoadingAddress = true,
                results = emptyList(),
                hasSearched = false,
                errorMessageRes = null
            )
        }

        // Only the last tap matters: cancel a lookup that is still waiting.
        addressJob?.cancel()
        addressJob = viewModelScope.launch {
            val address = repository.reverse(latitude, longitude).getOrDefault("")
            _uiState.update { state ->
                if (state.selected == place) {
                    state.copy(selected = place.copy(address = address), isLoadingAddress = false)
                } else {
                    state
                }
            }
        }
    }

    @StringRes
    private fun Throwable.toMessageRes(): Int = when (this) {
        is IOException -> R.string.error_network
        else -> R.string.error_search_failed
    }

    companion object {
        const val ARG_LATITUDE = "lat"
        const val ARG_LONGITUDE = "lon"
        const val ARG_ADDRESS = "address"
    }
}
