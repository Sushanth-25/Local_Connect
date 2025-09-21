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
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            Log.d(TAG, "Google Sign In successful: ${account.email}")

            // Get the ID token from the account
            val idToken = account.idToken
            if (idToken != null) {
                // Create a credential for Firebase Auth
                val credential = GoogleAuthProvider.getCredential(idToken, null)

                // Sign in to Firebase with the Google credential
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Firebase Auth with Google successful")
                            val user = FirebaseAuth.getInstance().currentUser

                            if (user != null) {
                                Log.d(TAG, "User registered: ${user.uid}")
                                // Navigate to the next screen on successful signup
                                onNavigateToLogin()
                            } else {
                                Log.e(TAG, "User is null after successful authentication")
                            }
                        } else {
                            Log.e(TAG, "Firebase Auth failed", task.exception)
                        }
                    }
            } else {
                Log.e(TAG, "Google Sign In failed: ID token is null")
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign In failed: ${e.statusCode}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign In unknown error", e)
        }
    }

    // Observe authResult for successful signup and navigate
    LaunchedEffect(authResult) {
        android.util.Log.d("SignupScreen", "Auth result changed: $authResult")
        if (authResult is AuthResult.Success) {
            android.util.Log.d("SignupScreen", "Auth success detected! Navigating to login...")
            viewModel.resetState()
            onNavigateToLogin()
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
                            if (name.isNotBlank() && email.isNotBlank() &&
                                password.isNotBlank() && password == confirmPassword) {
                                coroutineScope.launch {
                                    android.util.Log.d("SignupScreen", "Starting signup process")
                                    // First attempt to sign up
                                    viewModel.signupWithEmail(name, email, password)

                                    // Set a delayed navigation as a failsafe
                                    kotlinx.coroutines.delay(3000) // Wait 3 seconds
                                    if (authResult !is AuthResult.Error) {
                                        android.util.Log.d("SignupScreen", "Failsafe navigation triggered")
                                        handleSuccessfulSignup()
                                    }
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
                            Text("Sign Up", fontSize = 16.sp)
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
