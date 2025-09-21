package com.example.localconnect

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
    object Idle : AuthResult()
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult

    fun login(email: String, password: String) {
        _authResult.value = AuthResult.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = AuthResult.Success
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Login failed")
                }
            }
    }

    fun signup(email: String, password: String) {
        _authResult.value = AuthResult.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = AuthResult.Success
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Signup failed")
                }
            }
    }

    fun signupWithEmail(name: String, email: String, password: String) {
        _authResult.value = AuthResult.Loading

        viewModelScope.launch {
            try {
                // Create user account
                val authResult = withContext(Dispatchers.IO) {
                    auth.createUserWithEmailAndPassword(email, password).await()
                }

                val user = authResult.user ?: throw Exception("User creation failed")

                // Update profile with display name
                val profileUpdates = userProfileChangeRequest {
                    displayName = name
                }

                withContext(Dispatchers.IO) {
                    user.updateProfile(profileUpdates).await()
                }

                // Save user profile
                saveUserProfile(user.uid, name, email)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(e.message ?: "Signup failed")
            }
        }
    }

    private fun saveUserProfile(uid: String, name: String, email: String) {
        val userMap = mapOf(
            "name" to name,
            "email" to email,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                android.util.Log.d("AuthViewModel", "User profile saved successfully - setting Success state")
                _authResult.value = AuthResult.Success
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AuthViewModel", "Failed to save user profile: ${e.message}")
                _authResult.value = AuthResult.Error(e.message ?: "Failed to save user profile")
            }
    }

    fun resetState() {
        _authResult.value = AuthResult.Idle
    }
}
