package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StudentApplication
import com.example.data.auth.StudentAuthManager
import com.example.domain.model.UserAccount
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Specialized ViewModel handling user authentication, profile switching, and biometric/session security.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager: StudentAuthManager =
        (application as? StudentApplication)?.container?.authManager
            ?: StudentAuthManager.getInstance(application)

    val currentUser: StateFlow<UserAccount> = authManager.currentUser

    fun logout() {
        viewModelScope.launch {
            authManager.signOutUser()
        }
    }
}

