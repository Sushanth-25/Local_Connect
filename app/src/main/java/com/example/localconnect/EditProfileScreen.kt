package com.example.localconnect

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // State for form fields
    var name by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var phoneNumber by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // State for password visibility
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // State for feedback
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    // Load phone number from Firestore if available
    LaunchedEffect(Unit) {
        currentUser?.uid?.let { userId ->
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                if (userDoc.exists()) {
                    phoneNumber = userDoc.getString("phoneNumber") ?: ""
                }
            } catch (_: Exception) {
                // Failed to load data, but we can continue with empty field
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Edit Your Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Profile Information Section
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Profile Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Phone Number Field
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Password Section
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Change Password",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Current Password Field
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Current Password") },
                        trailingIcon = {
                            IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                Icon(
                                    if (currentPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (currentPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // New Password Field
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "New Password") },
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (newPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Confirm Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password") },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = newPassword != confirmPassword && confirmPassword.isNotEmpty()
                    )

                    if (newPassword != confirmPassword && confirmPassword.isNotEmpty()) {
                        Text(
                            text = "Passwords don't match",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Error and Success Messages
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (successMessage.isNotEmpty()) {
                Text(
                    text = successMessage,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Save Button
            Button(
                onClick = {
                    // Reset messages
                    errorMessage = ""
                    successMessage = ""
                    isLoading = true

                    // Use AtomicBoolean to track completion of async operations
                    val nameUpdated = java.util.concurrent.atomic.AtomicBoolean(false)
                    val phoneUpdated = java.util.concurrent.atomic.AtomicBoolean(false)
                    val passwordUpdated = java.util.concurrent.atomic.AtomicBoolean(false)
                    val passwordUpdateRequired = currentPassword.isNotEmpty() && newPassword.isNotEmpty() && newPassword == confirmPassword

                    // Function to check if all operations are complete and navigate
                    fun checkAllOperationsAndNavigate() {
                        if ((nameUpdated.get() || name.isBlank() || name == currentUser?.displayName) &&
                            phoneUpdated.get() &&
                            (passwordUpdated.get() || !passwordUpdateRequired)) {

                            successMessage = "Profile updated successfully!"
                            isLoading = false

                            // FIXED: Use immediate navigation with clear flags for more reliable behavior
                            navController.navigate("profile") {
                                popUpTo("profile") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    // Add a failsafe to ensure navigation happens even if operations take too long
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (isLoading) {
                            isLoading = false
                            successMessage = "Profile updated"
                            navController.navigate("profile") {
                                popUpTo("profile") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }, 5000) // 5-second timeout

                    // 1. Update Name in Firebase Auth Profile
                    if (name.isNotBlank() && name != currentUser?.displayName) {
                        currentUser?.updateProfile(
                            UserProfileChangeRequest.Builder().setDisplayName(name).build()
                        )?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                nameUpdated.set(true)
                            } else {
                                errorMessage = "Failed to update name"
                            }
                            checkAllOperationsAndNavigate()
                        }
                    } else {
                        nameUpdated.set(true) // Nothing to update
                    }

                    // 2. Save Phone Number to Firestore - Always attempt this operation
                    currentUser?.uid?.let { userId ->
                        val userData = mapOf(
                            "phoneNumber" to phoneNumber,
                            "uid" to userId,
                            "lastUpdated" to com.google.firebase.Timestamp.now()
                        )

                        // Use set with merge option to ensure the document is created if it doesn't exist
                        db.collection("users").document(userId)
                            .set(userData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener {
                                phoneUpdated.set(true)
                                checkAllOperationsAndNavigate()
                            }
                            .addOnFailureListener { e ->
                                errorMessage = "Failed to save phone number: ${e.message}"
                                isLoading = false
                            }
                    } ?: run {
                        // No user ID, mark as updated anyway
                        phoneUpdated.set(true)
                        checkAllOperationsAndNavigate()
                    }

                    // 3. Update Password if requested
                    if (passwordUpdateRequired) {
                        // Re-authenticate user before changing password
                        currentUser?.email?.let { email ->
                            val credential = EmailAuthProvider.getCredential(email, currentPassword)

                            currentUser.reauthenticate(credential)
                                .addOnSuccessListener {
                                    // Re-authentication successful, now change password
                                    currentUser.updatePassword(newPassword)
                                        .addOnSuccessListener {
                                            passwordUpdated.set(true)
                                            checkAllOperationsAndNavigate()
                                        }
                                        .addOnFailureListener { e ->
                                            errorMessage = "Failed to update password: ${e.message}"
                                            isLoading = false
                                        }
                                }
                                .addOnFailureListener {
                                    errorMessage = "Incorrect current password"
                                    isLoading = false
                                }
                        } ?: run {
                            // No email, mark password as updated anyway
                            passwordUpdated.set(true)
                            checkAllOperationsAndNavigate()
                        }
                    } else {
                        // No password update needed
                        passwordUpdated.set(true)
                        checkAllOperationsAndNavigate()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading &&
                         (name.isNotBlank() ||
                          phoneNumber.isNotBlank() ||
                          (currentPassword.isNotEmpty() && newPassword.isNotEmpty() && newPassword == confirmPassword))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(text = "Save Changes")
                }
            }
        }
    }
}
