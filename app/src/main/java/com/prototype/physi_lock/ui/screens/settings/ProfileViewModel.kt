package com.prototype.physi_lock.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prototype.physi_lock.data.PhysiLockDatabase
import com.prototype.physi_lock.data.UserConfiguration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val fullName: String = "John Doe",
    val username: String = "johndoe123",
    val email: String = "johndoe@gmail.com"
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val userConfigDao = PhysiLockDatabase.getInstance(application).userConfigurationDao()

    val profile: StateFlow<ProfileUiState> = userConfigDao.getActiveConfiguration()
        .map { config ->
            ProfileUiState(
                fullName = config?.fullName ?: "John Doe",
                username = config?.username ?: "johndoe123",
                email = config?.email ?: "johndoe@gmail.com"
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun updateProfile(fullName: String, username: String, email: String) {
        viewModelScope.launch {
            val current = userConfigDao.getActiveConfigurationOnce() ?: UserConfiguration()
            userConfigDao.upsert(
                current.copy(
                    fullName = fullName,
                    username = username,
                    email = email,
                    lastUpdatedTime = System.currentTimeMillis()
                )
            )
        }
    }
}
