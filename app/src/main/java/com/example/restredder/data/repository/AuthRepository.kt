package com.example.restredder.data.repository

import com.example.restredder.data.model.AppUser
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class AuthError { INVALID_CREDENTIALS, NETWORK, TOO_MANY_REQUESTS, UNKNOWN }

class AuthException(val error: AuthError, cause: Throwable? = null) : Exception(cause)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AuthException(e.toAuthError(), e))
        }
    }

    fun logout() = auth.signOut()

    /** Profile of the logged-in user (name, email, role), or null if it can't be loaded. */
    suspend fun getCurrentUser(): AppUser? {
        val user = auth.currentUser ?: return null
        return try {
            firestore.collection("users")
                .document(user.uid)
                .get()
                .await()
                .toObject(AppUser::class.java)
                ?.let { it.copy(email = it.email.ifBlank { user.email.orEmpty() }) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AuthException(e.toAuthError(), e))
        }
    }

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

    private fun Exception.toAuthError(): AuthError = when (this) {
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> AuthError.INVALID_CREDENTIALS
        is FirebaseNetworkException -> AuthError.NETWORK
        is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
        else -> AuthError.UNKNOWN
    }
}
