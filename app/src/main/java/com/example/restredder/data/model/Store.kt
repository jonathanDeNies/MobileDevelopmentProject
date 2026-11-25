package com.example.restredder.data.model

enum class StoreCategory(val key: String) {
    BAKERY("bakery"),
    SUPERMARKET("supermarket"),
    RESTAURANT("restaurant"),
    CAFE("cafe"),
    BUTCHER("butcher"),
    OTHER("other")
}

data class Store(
    val ownerId: String = "",
    val name: String = "",
    val category: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    // From Nominatim search or reverse geocoding.
    val address: String = ""
)
