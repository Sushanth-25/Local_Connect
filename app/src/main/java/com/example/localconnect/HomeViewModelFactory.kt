package com.example.localconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.localconnect.data.repository.FirebasePostRepository

class HomeViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                postRepository = FirebasePostRepository()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
