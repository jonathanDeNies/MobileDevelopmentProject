package com.example.restredder.data.repository

import com.example.restredder.data.model.Store
import com.example.restredder.data.model.StoreCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class StoreRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun createStore(
        name: String,
        category: StoreCategory,
        latitude: Double,
        longitude: Double,
        address: String,
        photoBase64: String
    ): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: throw AuthException(AuthError.UNKNOWN)

            val store = Store(
                ownerId = uid,
                name = name.trim(),
                category = category.key,
                latitude = latitude,
                longitude = longitude,
                address = address,
                photoBase64 = photoBase64
            )

            firestore.collection("stores")
                .add(store)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Permanently deletes a store. Firestore rules only allow this for the owner. */
    suspend fun deleteStore(storeId: String): Result<Unit> {
        return try {
            firestore.collection("stores")
                .document(storeId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Moves a store. Only the location fields change; Firestore rules only allow this for the owner. */
    suspend fun updateStoreLocation(
        storeId: String,
        latitude: Double,
        longitude: Double,
        address: String
    ): Result<Unit> {
        return try {
            firestore.collection("stores")
                .document(storeId)
                .update(
                    mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "address" to address
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Live list of the logged-in merchant's stores, sorted by name.
     * Emits again whenever a store is added, changed or removed.
     * Sorted in the app, so Firestore doesn't need a composite index.
     */
    fun observeMyStores(): Flow<List<Store>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("stores")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val stores = snapshot?.toObjects(Store::class.java).orEmpty()
                trySend(stores.sortedBy { it.name.lowercase() })
            }

        awaitClose { registration.remove() }
    }
}
