// app/src/main/java/com/example/localconnect/MainActivity.kt
package com.example.localconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.localconnect.ui.theme.LocalConnectTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.platform.LocalContext
class MainActivity : ComponentActivity() {
    private var isAuthFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { !isAuthFinished }

        setContent {
            LocalConnectTheme {
                MainActivityContent(
                    onAuthFinished = { isAuthFinished = true }
                )
            }
        }
    }
}

@Composable
fun MainActivityContent(onAuthFinished: () -> Unit) {
    val navController: NavHostController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    // Authenticate during splash
    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        // Check if user is valid (authenticated and not anonymous)
        if (user != null) {
            // Check if user signed in with Google (they don't need email verification)
            val isGoogleUser = user.providerData.any { it.providerId == "google.com" }

            if (isGoogleUser || user.isEmailVerified) {
                // Verify token is still valid (not expired)
                user.getIdToken(true)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful && !user.isAnonymous) {
                            // User is authenticated and token is valid
                            startDestination = "home"
                        } else {
                            // Token refresh failed or user is anonymous
                            startDestination = "login"
                        }
                        onAuthFinished()
                    }
                    .addOnFailureListener {
                        // Authentication validation failed
                        startDestination = "login"
                        onAuthFinished()
                    }
            } else {
                // User exists but email not verified (only for email/password users)
                startDestination = "email_verification/${user.email}"
                onAuthFinished()
            }
        } else {
            // No user found
            startDestination = "login"
            onAuthFinished()
        }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        PermissionUtils.saveLocationPermissionResult(context, isGranted)
        // Optionally handle result
    }

    // Request location permission after authentication, only if not asked before
    LaunchedEffect(startDestination) {
        if (startDestination != null && !PermissionUtils.wasLocationPermissionAsked(context)) {
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    // Show NavHost after authentication
    if (startDestination != null) {
        NavHost(
            navController = navController,
            startDestination = startDestination!!
        ) {
            composable("login") {
                LoginScreen(
                    onNavigateToSignup = { navController.navigate("signup") },
                    onLoginSuccess = { navController.navigate("home"){
                        popUpTo("login") { inclusive = true }
                    }},
                    onEmailNotVerified = { email ->
                        navController.navigate("email_verification/$email")
                    }
                )
            }
            composable("signup") {
                SignupScreen(
                    onNavigateToLogin = { navController.navigate("login") },
                    onNavigateToEmailVerification = { email ->
                        navController.navigate("email_verification/$email")
                    },
                    onNavigateToHome = {
                        navController.navigate("home") {
                            popUpTo("signup") { inclusive = true }
                        }
                    }
                )
            }
            composable("email_verification/{email}") { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                EmailVerificationScreen(
                    email = email,
                    onVerificationComplete = {
                        navController.navigate("home") {
                            popUpTo("email_verification/{email}") { inclusive = true }
                        }
                    },
                    onBackToLogin = {
                        navController.navigate("login") {
                            popUpTo("email_verification/{email}") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(navController = navController)
            }
            composable("profile") {
                ProfileScreen(navController = navController)
            }
        }
    }
}
