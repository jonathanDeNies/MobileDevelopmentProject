package com.example.restredder.ui.store

import androidx.annotation.StringRes
import com.example.restredder.R
import com.example.restredder.data.model.StoreCategory

/** Text shown for each category; the texts live in strings.xml. */
@StringRes
fun StoreCategory.labelRes(): Int = when (this) {
    StoreCategory.BAKERY -> R.string.category_bakery
    StoreCategory.SUPERMARKET -> R.string.category_supermarket
    StoreCategory.RESTAURANT -> R.string.category_restaurant
    StoreCategory.CAFE -> R.string.category_cafe
    StoreCategory.BUTCHER -> R.string.category_butcher
    StoreCategory.OTHER -> R.string.category_other
}
