package com.example.restredder.data.repository

import android.os.SystemClock
import com.example.restredder.data.model.PlaceResult
import com.example.restredder.data.remote.NominatimApi
import com.example.restredder.data.remote.NominatimClient
import com.example.restredder.data.remote.NominatimPlace
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class GeocodingRepository(
    private val api: NominatimApi = NominatimClient.api
) {
    suspend fun search(query: String): Result<List<PlaceResult>> = request {
        api.search(query = query.trim(), language = language())
            .mapNotNull { it.toPlaceResult() }
    }

    suspend fun reverse(latitude: Double, longitude: Double): Result<String> = request {
        api.reverse(latitude, longitude, language = language()).displayName.orEmpty()
    }

    private suspend fun <T> request(block: suspend () -> T): Result<T> {
        return try {
            Result.success(throttled(block))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun language(): String = Locale.getDefault().language

    private fun NominatimPlace.toPlaceResult(): PlaceResult? {
        val latitude = lat?.toDoubleOrNull() ?: return null
        val longitude = lon?.toDoubleOrNull() ?: return null
        return PlaceResult(latitude, longitude, displayName.orEmpty())
    }

    companion object {
        // Nominatim usage policy: at most 1 request per second (shared by the whole app).
        private const val MIN_INTERVAL_MS = 1_000L
        private val mutex = Mutex()
        private var lastRequestAt = 0L

        private suspend fun <T> throttled(block: suspend () -> T): T = mutex.withLock {
            val wait = lastRequestAt + MIN_INTERVAL_MS - SystemClock.elapsedRealtime()
            if (wait > 0) delay(wait)
            try {
                block()
            } finally {
                lastRequestAt = SystemClock.elapsedRealtime()
            }
        }
    }
}
