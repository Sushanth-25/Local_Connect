package com.example.localconnect

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

private const val TAG = "SignupScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToEmailVerification: (String) -> Unit,
    onNavigateToHome: () -> Unit, // Add direct navigation to home
    viewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val context = LocalContext.current
    val webClientId = context.getString(R.string.default_web_client_id)
    val authResult by viewModel.authResult.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Google Sign-In launcher
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "Google Sign-In result received with result code: ${result.resultCode}")

        if (result.resultCode == android.app.Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Google Sign In successful: ${account.email}")

                // Get the ID token from the account
                val idToken = account.idToken
                if (idToken != null) {
                    Log.d(TAG, "ID token received, authenticating with Firebase...")
                    // Create a credential for Firebase Auth
                    val credential = GoogleAuthProvider.getCredential(idToken, null)

                    // Use ViewModel's signInWithGoogle function
                    viewModel.signInWithGoogle(credential)
                } else {
                    Log.e(TAG, "Google Sign In failed: ID token is null")
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google Sign In failed with status code: ${e.statusCode}, message: ${e.message}", e)
                // Handle specific error codes
                when (e.statusCode) {
                    12501 -> Log.e(TAG, "User cancelled the sign-in")
                    7 -> Log.e(TAG, "Network error")
                    8 -> Log.e(TAG, "Internal error")
                    10 -> Log.e(TAG, "Developer error - check configuration")
                    else -> Log.e(TAG, "Unknown Google Sign-In error: ${e.statusCode}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign In unknown error", e)
            }
        } else if (result.resultCode == android.app.Activity.RESULT_CANCELED) {
            Log.d(TAG, "Google Sign-In was cancelled by user")
        } else {
            Log.e(TAG, "Google Sign-In failed with result code: ${result.resultCode}")
        }
    }

    // Observe authResult for successful signup and navigate
    LaunchedEffect(authResult) {
        android.util.Log.d("SignupScreen", "Auth result changed: $authResult")
        when (authResult) {
            is AuthResult.Success -> {
                android.util.Log.d("SignupScreen", "Auth success detected! Navigating to home...")
                viewModel.resetState()
                onNavigateToHome() // Navigate directly to home for successful authentication
            }
            is AuthResult.EmailVerificationSent -> {
                android.util.Log.d("SignupScreen", "Email verification sent! Navigating to verification screen...")
                onNavigateToEmailVerification(email)
            }
            else -> { /* Handle other states */ }
        }
    }

    // Direct navigation handler - can be triggered from button too
    fun handleSuccessfulSignup() {
        android.util.Log.d("SignupScreen", "Handling successful signup")
        viewModel.resetState()
        onNavigateToLogin()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo/Title
            Text(
                text = "Local Connect",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Join your local community",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Google Sign Up Button
            ElevatedButton(
                onClick = {
                    // Google Sign-In
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    launcher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sign up with Google",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Text(
                    text = "OR",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Divider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign Up Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                ) {
                    Text(
                        text = "Create an account",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Name field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Name"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password"
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Confirm Password"
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (authResult is AuthResult.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (authResult as AuthResult.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign Up Button
                    Button(
                        onClick = {
                            // Validate all fields
                            when {
                                name.isBlank() -> {
                                    // Show error for empty name
                                }
                                email.isBlank() -> {
                                    // Show error for empty email
                                }
                                password.isBlank() -> {
                                    // Show error for empty password
                                }
                                password != confirmPassword -> {
                                    // Show error for password mismatch
                                }
                                password.length < 6 -> {
                                    // Show error for weak password
                                }
                                else -> {
                                    android.util.Log.d("SignupScreen", "Starting signup with email verification")
                                    viewModel.signupWithEmailVerification(name, email, password)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = authResult !is AuthResult.Loading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (authResult is AuthResult.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Create Account",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // Show verification message if email verification was sent
                    if (authResult is AuthResult.EmailVerificationSent) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verification email sent! Please check your inbox.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Link
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Already have an account? Log in",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Add extra space at the bottom for scrolling
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Show loading indicator if needed
        if (authResult is AuthResult.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
