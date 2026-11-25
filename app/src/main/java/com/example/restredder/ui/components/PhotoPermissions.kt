package com.example.restredder.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

enum class PhotoSource { GALLERY, CAMERA }

/** The permissions to request for each photo source, depending on the Android version. */
fun PhotoSource.permissions(): Array<String> = when (this) {
    PhotoSource.CAMERA -> arrayOf(Manifest.permission.CAMERA)
    PhotoSource.GALLERY -> when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

/**
 * True when access is granted. For the gallery, "Allow only selected photos"
 * (Android 14+) also counts.
 */
fun hasPhotoPermission(context: Context, source: PhotoSource): Boolean =
    source.permissions().any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/**
 * False after the user denied twice (or chose "Don't ask again"):
 * Android then no longer shows the permission dialog.
 */
fun canAskAgain(context: Context, source: PhotoSource): Boolean {
    val activity = context.findActivity() ?: return true
    return source.permissions().any {
        ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
    }
}

private const val PREFS_NAME = "photo_permissions"

/**
 * Remembers that Android won't show its permission dialog anymore for this source
 * (denied twice). Android doesn't tell us this before asking, so we store it ourselves.
 */
fun setPermissionBlocked(context: Context, source: PhotoSource, blocked: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(source.name, blocked)
        .apply()
}

fun isPermissionBlocked(context: Context, source: PhotoSource): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(source.name, false)

/** Opens this app's page in the system settings, where permissions can be turned on. */
fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
