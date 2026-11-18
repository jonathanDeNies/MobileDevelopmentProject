package com.example.restredder.ui.home

import androidx.lifecycle.ViewModel
import com.example.restredder.data.repository.AuthRepository

class HomeViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    fun logout() = repository.logout()
}
