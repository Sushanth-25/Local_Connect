package com.example.localconnect.presentation.staff

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localconnect.viewmodel.StaffViewModel
import com.example.localconnect.viewmodel.StaffAuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffLoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: StaffViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    // Handle authentication results
    LaunchedEffect(authState) {
        when (authState) {
            is StaffAuthState.Success -> {
                Toast.makeText(context, "Staff login successful", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
                viewModel.resetAuthState()
            }
            is StaffAuthState.NotStaff -> {
                Toast.makeText(context, "This account is not authorized as staff", Toast.LENGTH_LONG).show()
                viewModel.resetAuthState()
            }
            is StaffAuthState.Error -> {
                Toast.makeText(context, (authState as StaffAuthState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetAuthState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1976D2),
                        Color(0xFF2196F3)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Staff Badge Icon
            Icon(
                imageVector = Icons.Default.Badge,
                contentDescription = "Staff Login",
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Staff Portal",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Authorized Personnel Only",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Login Form Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Staff Email") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1976D2),
                            focusedLabelColor = Color(0xFF1976D2)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
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
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1976D2),
                            focusedLabelColor = Color(0xFF1976D2)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                viewModel.staffLogin(email, password)
                            } else {
                                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = authState != StaffAuthState.Loading
                    ) {
                        if (authState == StaffAuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "LOGIN AS STAFF",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Back to User Login
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Back to User Login",
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3CD)
                )
            ) {
                Text(
                    text = "⚠️ This portal is for authorized staff members only. All activities are logged and monitored.",
                    fontSize = 12.sp,
                    color = Color(0xFF856404),
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

