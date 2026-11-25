package com.example.restredder.ui.store

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.restredder.R
import com.example.restredder.data.model.PlaceResult
import com.example.restredder.data.model.StoreCategory
import com.example.restredder.ui.components.Base64Image
import com.example.restredder.ui.components.MapPicker
import com.example.restredder.ui.components.PhotoSource
import com.example.restredder.ui.components.canAskAgain
import com.example.restredder.ui.components.hasPhotoPermission
import com.example.restredder.ui.components.isPermissionBlocked
import com.example.restredder.ui.components.openAppSettings
import com.example.restredder.ui.components.permissions
import com.example.restredder.ui.components.setPermissionBlocked

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoreScreen(
    onBack: () -> Unit,
    onChooseLocation: (current: PlaceResult?) -> Unit,
    onStoreCreated: () -> Unit,
    viewModel: CreateStoreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPhotoSheet by rememberSaveable { mutableStateOf(false) }

    // Source the Android permission dialog is currently open for.
    var requestedSource by rememberSaveable { mutableStateOf<PhotoSource?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(viewModel::onPhotoPicked) }

    // The camera app writes the photo into a temporary file we provide.
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraUri?.let(viewModel::onPhotoPicked) }

    val openSource: (PhotoSource) -> Unit = { source ->
        when (source) {
            PhotoSource.GALLERY -> galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            PhotoSource.CAMERA -> {
                val uri = viewModel.createCameraUri()
                cameraUri = uri
                try {
                    cameraLauncher.launch(uri)
                } catch (e: ActivityNotFoundException) {
                    viewModel.onCameraUnavailable()
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val source = requestedSource ?: return@rememberLauncherForActivityResult
        requestedSource = null
        val isCamera = source == PhotoSource.CAMERA
        when {
            // For the gallery, "only selected photos" (Android 14+) is enough.
            results.values.any { it } -> {
                setPermissionBlocked(context, source, blocked = false)
                openSource(source)
            }
            // Android won't show its dialog anymore: next time we go to the settings.
            !canAskAgain(context, source) -> {
                setPermissionBlocked(context, source, blocked = true)
                viewModel.onPhotoPermissionBlocked(isCamera)
            }
            else -> viewModel.onPhotoPermissionDenied(isCamera)
        }
    }

    // Tapping camera or gallery asks Android's permission dialog right away.
    val onSourceChosen: (PhotoSource) -> Unit = { source ->
        showPhotoSheet = false
        when {
            hasPhotoPermission(context, source) -> {
                setPermissionBlocked(context, source, blocked = false)
                openSource(source)
            }
            isPermissionBlocked(context, source) -> openAppSettings(context)
            else -> {
                requestedSource = source
                permissionLauncher.launch(source.permissions())
            }
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onStoreCreated()
    }

    if (showPhotoSheet) {
        PhotoSourceSheet(
            onDismiss = { showPhotoSheet = false },
            onGallery = { onSourceChosen(PhotoSource.GALLERY) },
            onCamera = { onSourceChosen(PhotoSource.CAMERA) }
        )
    }

    Scaffold(
        // The activity already applies the system bar padding.
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_store_title)) },
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
            SaveBar(
                isLoading = uiState.isLoading,
                canSave = uiState.canSave,
                errorMessageRes = uiState.errorMessageRes,
                onSave = viewModel::createStore
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.store_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle(R.string.store_category_label)

            CategoryChips(
                selected = uiState.category,
                onSelected = viewModel::onCategorySelected
            )

            SectionTitle(R.string.store_photo_label)

            PhotoCard(
                photoBase64 = uiState.photoBase64,
                isProcessing = uiState.isProcessingPhoto,
                onClick = { showPhotoSheet = true },
                onRemove = viewModel::removePhoto
            )

            uiState.photoErrorRes?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(it),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Inline link (no extra window) when the permission can only be turned on in settings.
            if (uiState.showOpenSettings) {
                TextButton(onClick = { openAppSettings(context) }) {
                    Text(stringResource(R.string.open_settings_button))
                }
            }

            SectionTitle(R.string.store_location_label)

            LocationCard(
                location = uiState.location,
                onClick = { onChooseLocation(uiState.location) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PhotoCard(
    photoBase64: String,
    isProcessing: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = !isProcessing,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isProcessing -> CircularProgressIndicator()

                photoBase64.isBlank() -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddAPhoto,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.add_photo_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.add_photo_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    Base64Image(
                        base64 = photoBase64,
                        contentDescription = stringResource(R.string.store_photo_preview),
                        modifier = Modifier.matchParentSize()
                    )

                    FilledTonalIconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.remove_photo)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.change_photo),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceSheet(
    onDismiss: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.choose_from_gallery)) },
                leadingContent = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = onGallery)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.take_photo)) },
                leadingContent = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = onCamera)
            )
        }
    }
}

@Composable
private fun SectionTitle(@StringRes textRes: Int) {
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    selected: StoreCategory?,
    onSelected: (StoreCategory) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StoreCategory.entries.forEach { category ->
            val isSelected = category == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(category) },
                label = { Text(stringResource(category.labelRes())) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun LocationCard(
    location: PlaceResult?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (location == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.choose_location_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.choose_location_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                MapPicker(
                    selectedLatitude = location.latitude,
                    selectedLongitude = location.longitude,
                    interactive = false,
                    modifier = Modifier.matchParentSize()
                )
                // Catches the tap above the (non-interactive) map.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onClick)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = location.address.ifBlank {
                            stringResource(
                                R.string.coordinates_format,
                                location.latitude,
                                location.longitude
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.change_location),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveBar(
    isLoading: Boolean,
    canSave: Boolean,
    @StringRes errorMessageRes: Int?,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        errorMessageRes?.let {
            Text(
                text = stringResource(it),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.save_store_button))
            }
        }
    }
}
