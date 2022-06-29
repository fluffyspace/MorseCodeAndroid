package com.example.morsecode.sockets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainViewModelFactory() : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(MainInteractor(MainRepository(WebServicesProvider()))) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}