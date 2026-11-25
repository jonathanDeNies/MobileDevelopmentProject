package com.example.restredder.data.repository

import com.example.restredder.data.model.Store
import com.example.restredder.data.model.StoreCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
        address: String
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
                address = address
            )

            firestore.collection("stores")
                .add(store)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
