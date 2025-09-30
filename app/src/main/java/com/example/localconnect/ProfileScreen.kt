package com.example.localconnect

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val name=currentUser?.displayName ?: "User Name"
    val email=currentUser?.email ?: "User Email"
    val phone=currentUser?.phoneNumber ?: "Phone Number"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User icon
            Icon(
                Icons.Default.Person,
                contentDescription = "Profile",
                modifier = Modifier.size(80.dp)
            )

            // User email
            Text(
                text = name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            //Email Container
            Surface (
                modifier=Modifier.fillMaxWidth(),
                shape=MaterialTheme.shapes.medium,
                tonalElevation = 2.dp
            ){
                Row(
                    modifier=Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically

                ){
                    Text(
                        text="Email: ",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text=email)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile options
            ProfileOption(
                title = "Edit Profile",
                icon = Icons.Default.Edit,
                onClick = { /* Handle edit profile */ }
            )

            Divider()

            Button(
                onClick = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}

@Composable
fun ProfileOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
