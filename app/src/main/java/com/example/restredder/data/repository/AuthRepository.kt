package com.example.restredder.data.repository

import com.example.restredder.data.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: String
    ): Result<Unit> {
        return try {
            require(name.isNotBlank()) {
                "Name is required"
            }

            require(role == "customer" || role == "merchant") {
                "Invalid role"
            }

            val cleanEmail = email.trim()

            val result = auth.createUserWithEmailAndPassword(
                cleanEmail,
                password
            ).await()

            val uid = result.user?.uid
                ?: error("Registration failed: missing UID")

            val user = AppUser(
                name = name.trim(),
                email = cleanEmail,
                role = role
            )

            firestore.collection("users")
                .document(uid)
                .set(user)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
