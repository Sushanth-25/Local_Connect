package com.example.localconnect

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.localconnect.data.repository.FirebaseAuthRepository
import com.example.localconnect.util.CloudinaryManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { FirebaseAuthRepository() }

    var name by remember { mutableStateOf(currentUser?.displayName ?: "User Name") }
    val email = currentUser?.email ?: "User Email"
    var phoneNumber by remember { mutableStateOf("Phone Number") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var showWelcomeDialog by remember { mutableStateOf(false) }

    // UCrop result launcher - handles the cropped image
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { croppedUri ->
                scope.launch {
                    isUploading = true
                    try {
                        val croppedFile = File(croppedUri.path!!)

                        // Upload cropped image to Cloudinary
                        val uploadedUrl = withContext(Dispatchers.IO) {
                            CloudinaryManager.uploadProfilePicture(croppedFile.absolutePath)
                        }

                        // Clean up cropped file
                        croppedFile.delete()

                        if (uploadedUrl != null) {
                            // Update Firestore
                            currentUser?.uid?.let { userId ->
                                authRepository.updateProfileImage(userId, uploadedUrl)
                                    .onSuccess {
                                        profileImageUrl = uploadedUrl
                                    }
                            }
                        }
                    } catch (_: Exception) {
                        // Handle error silently
                    } finally {
                        isUploading = false
                    }
                }
            }
        }
    }

    // Image picker launcher - opens cropper after image selection
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            // Create destination file for cropped image
            val destinationUri = Uri.fromFile(
                File(context.cacheDir, "cropped_profile_${System.currentTimeMillis()}.jpg")
            )

            // Configure and start UCrop
            val options = UCrop.Options().apply {
                setCompressionQuality(90)
                setCircleDimmedLayer(true) // Circular crop overlay
                setShowCropGrid(true)
                setShowCropFrame(true)
                setToolbarTitle("Crop Profile Picture")

                // Set colors for better visibility
                setStatusBarColor(context.getColor(android.R.color.black))
                setToolbarColor(context.getColor(R.color.purple_700))
                setToolbarWidgetColor(context.getColor(android.R.color.white))
                setActiveControlsWidgetColor(context.getColor(R.color.purple_500))
                setRootViewBackgroundColor(context.getColor(android.R.color.black))

                // Hide bottom controls for simpler interface
                setHideBottomControls(false)
                setFreeStyleCropEnabled(false) // Keep aspect ratio locked
            }

            val uCrop = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f) // Square aspect ratio
                .withMaxResultSize(800, 800) // Max 800x800 pixels
                .withOptions(options)

            cropLauncher.launch(uCrop.getIntent(context))
        }
    }

    // Fetch user data from Firestore
    LaunchedEffect(Unit) {
        currentUser?.uid?.let { userId ->
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                if (userDoc.exists()) {
                    phoneNumber = userDoc.getString("phoneNumber") ?: "Phone Number"
                    name = userDoc.getString("name") ?: currentUser.displayName ?: "User Name"
                    profileImageUrl = userDoc.getString("profileImage")

                    // Check if this is a new user (no profile image and account created recently)
                    val createdAt = userDoc.getLong("createdAt") ?: 0L
                    val isNewUser = profileImageUrl.isNullOrEmpty() &&
                            (System.currentTimeMillis() - createdAt) < 60000 // Within 1 minute

                    if (isNewUser) {
                        showWelcomeDialog = true
                    }
                }
            } catch (_: Exception) {
                // Failed to load data, keep default value
            } finally {
                isLoading = false
            }
        } ?: run {
            isLoading = false
        }
    }

    // Welcome dialog for new users
    if (showWelcomeDialog) {
        AlertDialog(
            onDismissRequest = { showWelcomeDialog = false },
            title = { Text("Welcome to LocalConnect! 👋") },
            text = {
                Text("Complete your profile to get started. Add a profile picture so others can recognize you in the community!")
            },
            confirmButton = {
                TextButton(onClick = {
                    showWelcomeDialog = false
                    imagePickerLauncher.launch("image/*")
                }) {
                    Text("Add Photo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWelcomeDialog = false }) {
                    Text("Later")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Profile picture with upload button
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(80.dp))
                } else {
                    // Profile Image
                    if (profileImageUrl.isNullOrEmpty()) {
                        // Default avatar
                        Image(
                            painter = painterResource(id = R.drawable.ic_avatar_placeholder),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    } else {
                        // User's profile picture
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.ic_avatar_placeholder)
                        )
                    }

                    // Camera button overlay
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Change Profile Picture",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // User name
            Text(
                text = name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            // Email Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Email: ",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = email)
                }
            }

            // Phone Number Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Phone",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Phone: ",
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = phoneNumber)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile options
            ProfileOption(
                title = "Edit Profile",
                icon = Icons.Default.Edit,
                onClick = { navController.navigate("edit_profile") }
            )

            HorizontalDivider()

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
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out")
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
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
