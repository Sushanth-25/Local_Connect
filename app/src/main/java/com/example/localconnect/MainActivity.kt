// app/src/main/java/com/example/localconnect/MainActivity.kt
package com.example.localconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
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

    // Authenticate during splash
    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        // Check if user is valid (authenticated and not anonymous)
        if (user != null) {
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
            // No user found
            startDestination = "login"
            onAuthFinished()
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
                    onLogin = { email, password -> /* Add your login logic here */ }
                )
            }
            composable("signup") {
                SignupScreen(
                    onNavigateToLogin = { navController.navigate("login") }
                )
            }
            composable("home") {
                HomeScreen() // Implement HomeScreen as needed
            }
        }
    }
}
