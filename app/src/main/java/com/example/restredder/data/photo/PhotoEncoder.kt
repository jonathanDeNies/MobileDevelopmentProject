package com.example.restredder.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt

class PhotoTooLargeException : Exception()

/**
 * Turns a photo into a small Base64 JPEG that fits in a Firestore document:
 * rotated upright, max 800 px on the longest side, JPEG quality 70.
 */
class PhotoEncoder(context: Context) {

    private val appContext = context.applicationContext

    suspend fun encode(uri: Uri): Result<String> = withContext(Dispatchers.Default) {
        try {
            val decoded = decodeSampled(uri) ?: error("Could not decode image")
            val upright = rotate(decoded, readRotationDegrees(uri))
            val scaled = scaleDown(upright)

            val bytes = ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.toByteArray()
            }
            if (bytes.size > MAX_JPEG_BYTES) throw PhotoTooLargeException()

            Result.success(Base64.encodeToString(bytes, Base64.NO_WRAP))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Empty file the camera app can write the new photo into. */
    fun createCameraUri(): Uri {
        val dir = File(appContext.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "store_photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
    }

    /** Decodes a smaller version right away, so huge camera photos don't use too much memory. */
    private fun decodeSampled(uri: Uri): Bitmap? {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sampleSize = 1
        val longest = max(bounds.outWidth, bounds.outHeight)
        while (longest / (sampleSize * 2) >= MAX_SIZE_PX) sampleSize *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    /** Phone cameras often save photos sideways and store the rotation in EXIF. */
    private fun readRotationDegrees(uri: Uri): Int {
        val orientation = appContext.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= MAX_SIZE_PX) return bitmap
        val factor = MAX_SIZE_PX.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * factor).roundToInt(),
            (bitmap.height * factor).roundToInt(),
            true
        )
    }

    private companion object {
        const val MAX_SIZE_PX = 800
        const val JPEG_QUALITY = 70

        // Base64 makes the data ~33% bigger; this keeps the store document well below 1 MiB.
        const val MAX_JPEG_BYTES = 500_000
    }
}
