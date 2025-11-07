package com.example.localconnect

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
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
    object EmailVerificationSent : AuthResult()
    object EmailVerified : AuthResult()
    object EmailNotVerified : AuthResult()
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult

    // Email verification state
    private val _verificationState = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val verificationState: StateFlow<AuthResult> = _verificationState

    fun login(email: String, password: String) {
        _authResult.value = AuthResult.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    // Check if user signed in with Google (they don't need email verification)
                    val isGoogleUser = user?.providerData?.any { it.providerId == "google.com" } ?: false

                    if (isGoogleUser || user?.isEmailVerified == true) {
                        _authResult.value = AuthResult.Success
                    } else {
                        _authResult.value = AuthResult.EmailNotVerified
                    }
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
                    sendEmailVerification()
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Signup failed")
                }
            }
    }

    fun signupWithEmailVerification(name: String, email: String, password: String) {
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

                // Send email verification
                withContext(Dispatchers.IO) {
                    user.sendEmailVerification().await()
                }

                // Save user profile (but mark as unverified)
                saveUserProfile(user.uid, name, email, false)

                _authResult.value = AuthResult.EmailVerificationSent

            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(e.message ?: "Signup failed")
            }
        }
    }

    fun sendEmailVerification() {
        val user = auth.currentUser
        if (user != null) {
            _verificationState.value = AuthResult.Loading
            user.sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _verificationState.value = AuthResult.EmailVerificationSent
                    } else {
                        _verificationState.value = AuthResult.Error(
                            task.exception?.message ?: "Failed to send verification email"
                        )
                    }
                }
        } else {
            _verificationState.value = AuthResult.Error("No user logged in")
        }
    }

    fun checkEmailVerification() {
        val user = auth.currentUser
        if (user != null) {
            _verificationState.value = AuthResult.Loading
            user.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (user.isEmailVerified) {
                        // Update user profile to mark as verified
                        updateUserVerificationStatus(user.uid, true)
                        _verificationState.value = AuthResult.EmailVerified
                        _authResult.value = AuthResult.Success
                    } else {
                        _verificationState.value = AuthResult.EmailNotVerified
                    }
                } else {
                    _verificationState.value = AuthResult.Error("Failed to check verification status")
                }
            }
        }
    }

    private fun updateUserVerificationStatus(uid: String, isVerified: Boolean) {
        firestore.collection("users").document(uid)
            .update("emailVerified", isVerified, "verifiedAt", System.currentTimeMillis())
            .addOnFailureListener { e ->
                android.util.Log.e("AuthViewModel", "Failed to update verification status: ${e.message}")
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
                saveUserProfile(user.uid, name, email, false)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(e.message ?: "Signup failed")
            }
        }
    }

    private fun saveUserProfile(uid: String, name: String, email: String, isVerified: Boolean = false) {
        val userMap = mapOf(
            "name" to name,
            "email" to email,
            "emailVerified" to isVerified,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                android.util.Log.d("AuthViewModel", "User profile saved successfully")
                if (_authResult.value != AuthResult.EmailVerificationSent) {
                    _authResult.value = AuthResult.Success
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AuthViewModel", "Failed to save user profile: ${e.message}")
                _authResult.value = AuthResult.Error(e.message ?: "Failed to save user profile")
            }
    }

    fun resetState() {
        _authResult.value = AuthResult.Idle
        _verificationState.value = AuthResult.Idle
    }

    fun resetVerificationState() {
        _verificationState.value = AuthResult.Idle
    }

    fun signInWithGoogle(credential: AuthCredential) {
        android.util.Log.d("AuthViewModel", "Starting Google sign-in with credential")
        _authResult.value = AuthResult.Loading

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("AuthViewModel", "Firebase Auth with Google successful")
                    val user = auth.currentUser
                    if (user != null) {
                        val name = user.displayName ?: ""
                        val email = user.email ?: ""
                        val uid = user.uid

                        // Check if user exists in Firestore, if not create the profile
                        firestore.collection("users").document(uid).get()
                            .addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    // User doesn't exist in Firestore, create new profile
                                    android.util.Log.d("AuthViewModel", "User not found in Firestore, creating new profile...")
                                    saveUserProfile(uid, name, email, true)
                                } else {
                                    // User already exists, just complete sign-in
                                    android.util.Log.d("AuthViewModel", "User already exists in Firestore")
                                    _authResult.value = AuthResult.Success
                                }
                            }
                            .addOnFailureListener { e ->
                                // On error checking, create the profile anyway to be safe
                                android.util.Log.e("AuthViewModel", "Error checking user existence: ${e.message}, creating profile anyway")
                                saveUserProfile(uid, name, email, true)
                            }
                    } else {
                        _authResult.value = AuthResult.Error("User is null after sign-in")
                    }
                } else {
                    android.util.Log.e("AuthViewModel", "Firebase Auth with Google failed: ${task.exception?.message}")
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Google sign-in failed")
                }
            }
    }
}
