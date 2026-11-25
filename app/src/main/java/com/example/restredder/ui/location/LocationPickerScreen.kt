package com.example.restredder.ui.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.restredder.R
import com.example.restredder.data.model.PlaceResult
import com.example.restredder.ui.components.MapPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    onBack: () -> Unit,
    onLocationConfirmed: (PlaceResult) -> Unit,
    viewModel: LocationPickerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        // The activity already applies the system bar padding.
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.location_picker_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        bottomBar = {
            SelectedLocationBar(
                selected = uiState.selected,
                isLoadingAddress = uiState.isLoadingAddress,
                onConfirm = { uiState.selected?.let(onLocationConfirmed) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            MapPicker(
                selectedLatitude = uiState.selected?.latitude,
                selectedLongitude = uiState.selected?.longitude,
                onLocationPicked = { lat, lon ->
                    keyboardController?.hide()
                    viewModel.onMapTapped(lat, lon)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Search bar and results float on top of the map.
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    TextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text(stringResource(R.string.search_address_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            when {
                                uiState.isSearching -> CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                uiState.query.isNotEmpty() -> IconButton(onClick = viewModel::clearSearch) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = stringResource(R.string.clear_search)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController?.hide()
                            viewModel.search()
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val message = when {
                    uiState.errorMessageRes != null -> stringResource(uiState.errorMessageRes!!)
                    uiState.hasSearched && !uiState.isSearching && uiState.results.isEmpty() ->
                        stringResource(R.string.no_search_results)
                    else -> null
                }

                if (uiState.results.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SearchResults(
                        results = uiState.results,
                        onResultClick = viewModel::onResultSelected
                    )
                } else if (message != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message,
                            color = if (uiState.errorMessageRes != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    results: List<PlaceResult>,
    onResultClick: (PlaceResult) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
            items(results) { place ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = place.address,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onResultClick(place) }
                )
                if (place != results.last()) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SelectedLocationBar(
    selected: PlaceResult?,
    isLoadingAddress: Boolean,
    onConfirm: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(16.dp)
        ) {
            if (selected == null) {
                Text(
                    text = stringResource(R.string.pick_location_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.selected_location_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when {
                                isLoadingAddress -> stringResource(R.string.looking_up_address)
                                selected.address.isNotBlank() -> selected.address
                                else -> stringResource(
                                    R.string.coordinates_format,
                                    selected.latitude,
                                    selected.longitude
                                )
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onConfirm,
                enabled = selected != null && !isLoadingAddress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(stringResource(R.string.use_location_button))
            }
        }
    }
}
