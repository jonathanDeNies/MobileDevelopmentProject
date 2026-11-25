package com.example.restredder.data.model

import com.google.firebase.firestore.DocumentId

enum class StoreCategory(val key: String) {
    BAKERY("bakery"),
    SUPERMARKET("supermarket"),
    RESTAURANT("restaurant"),
    CAFE("cafe"),
    BUTCHER("butcher"),
    OTHER("other");

    companion object {
        fun fromKey(key: String): StoreCategory =
            entries.firstOrNull { it.key == key } ?: OTHER
    }
}

data class Store(
    // Filled in by Firestore when reading; ignored when writing.
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val category: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    // From Nominatim search or reverse geocoding.
    val address: String = "",
    // Optional JPEG photo (max 800 px, quality 70) as Base64; "" when there is no photo.
    val photoBase64: String = ""
)
