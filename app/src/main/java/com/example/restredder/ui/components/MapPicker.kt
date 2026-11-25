package com.example.restredder.ui.components

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.restredder.AppInfo
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

// Center of Belgium, used when no location is selected yet.
private const val DEFAULT_LATITUDE = 50.85
private const val DEFAULT_LONGITUDE = 4.35
private const val DEFAULT_ZOOM = 8.0
private const val PIN_ZOOM = 16.0
private const val ANIMATION_MS = 600L

/**
 * OpenStreetMap view with a single pin.
 *
 * - interactive = true: the user can pan/zoom and tap to place or move the pin.
 * - interactive = false: a static preview (e.g. inside a card); touches are ignored.
 *
 * When the selected location changes, the map moves to it.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapPicker(
    selectedLatitude: Double?,
    selectedLongitude: Double?,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onLocationPicked: (latitude: Double, longitude: Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnLocationPicked by rememberUpdatedState(onLocationPicked)

    val mapView = remember {
        Configuration.getInstance().userAgentValue = AppInfo.USER_AGENT

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            // Required attribution: "© OpenStreetMap contributors".
            overlays.add(CopyrightOverlay(context))

            if (interactive) {
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            } else {
                setMultiTouchControls(false)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                setOnTouchListener { _, _ -> true }
            }

            if (selectedLatitude != null && selectedLongitude != null) {
                controller.setZoom(PIN_ZOOM)
                controller.setCenter(GeoPoint(selectedLatitude, selectedLongitude))
            } else {
                controller.setZoom(DEFAULT_ZOOM)
                controller.setCenter(GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
            }
        }
    }

    val marker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setInfoWindow(null)
        }
    }

    // Remembers the last pin position the map moved to, so it only moves on a real change.
    val lastShown = remember {
        arrayOf(
            if (selectedLatitude != null && selectedLongitude != null) {
                GeoPoint(selectedLatitude, selectedLongitude)
            } else {
                null
            }
        )
    }

    DisposableEffect(lifecycle, mapView) {
        val tapOverlay = if (interactive) {
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                    currentOnLocationPicked(point.latitude, point.longitude)
                    return true
                }

                override fun longPressHelper(point: GeoPoint): Boolean = false
            }).also { mapView.overlays.add(0, it) }
        } else {
            null
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            tapOverlay?.let { mapView.overlays.remove(it) }
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        // MapView draws outside its bounds in Compose without clipping.
        modifier = modifier.clipToBounds(),
        update = { map ->
            if (selectedLatitude != null && selectedLongitude != null) {
                val point = GeoPoint(selectedLatitude, selectedLongitude)
                marker.position = point
                if (!map.overlays.contains(marker)) map.overlays.add(marker)

                if (lastShown[0] != point) {
                    lastShown[0] = point
                    when {
                        map.zoomLevelDouble < PIN_ZOOM ->
                            map.controller.animateTo(point, PIN_ZOOM, ANIMATION_MS)
                        !map.boundingBox.contains(point) ->
                            map.controller.animateTo(point)
                    }
                }
            } else {
                map.overlays.remove(marker)
                lastShown[0] = null
            }
            map.invalidate()
        }
    )
}
