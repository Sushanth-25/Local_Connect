package com.example.localconnect.presentation.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.localconnect.data.model.PostType
import com.example.localconnect.presentation.viewmodel.CreatePostViewModel
import com.example.localconnect.presentation.viewmodel.CreatePostViewModelFactory
import com.example.localconnect.util.PermissionUtils
import com.example.localconnect.util.UserLocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.location.Address
import android.location.Geocoder
import java.util.Locale
import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    navController: NavController,
    viewModel: CreatePostViewModel = viewModel(factory = CreatePostViewModelFactory(LocalContext.current.applicationContext as Application)),
    onPostCreated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showPostTypeDropdown by remember { mutableStateOf(false) }
    var showPriorityDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }

    // Camera image file state
    val cameraImageFile = remember { mutableStateOf<File?>(null) }
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

    // Media launchers - Updated to use TakePicture for full resolution
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            cameraImageUri.value?.let { uri ->
                // Add the captured image URI to the list
                val newList = uiState.imageUris.toMutableList().apply { add(uri) }
                viewModel.onImageUrisChange(newList)
                scope.launch { snackbarHostState.showSnackbar("Photo captured successfully") }
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Failed to capture photo") }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        // Replace current selection with picked items
        viewModel.onImageUrisChange(uris)
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onVideoUriChange(uri)
    }

    // Permission launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        PermissionUtils.saveCameraPermissionResult(context, isGranted)
        if (isGranted) {
            // Permission granted, create image file and launch camera
            try {
                val file = createImageFile(context)
                cameraImageFile.value = file
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                cameraImageUri.value = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Failed to open camera: ${e.message}") }
            }
        } else {
            // Permission denied, show appropriate message
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission is required to take photos")
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        PermissionUtils.saveLocationPermissionResult(context, fineLocationGranted || coarseLocationGranted)

        if (fineLocationGranted || coarseLocationGranted) {
            // Get current location after permission is granted
            getUserCurrentLocation(context, viewModel, snackbarHostState, scope)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Location permission is required to add location")
            }
        }
    }

    // Handle navigation result for location: observe savedStateHandle StateFlow so we react when MapScreen sets it
    LaunchedEffect(navController) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        val flow = savedStateHandle?.getStateFlow("selected_location", "")
        if (flow != null) {
            flow.collect { locationString ->
                if (locationString.isBlank()) return@collect

                val resolved = try {
                    val coords = locationString.split(",").mapNotNull { it.trim().toDoubleOrNull() }
                    if (coords.size >= 2) {
                        androidGeocodeBestName(context, coords[0], coords[1]) ?: locationString
                    } else locationString
                } catch (_: Exception) { locationString }

                viewModel.onLocationChange(resolved)
                val coords = locationString.split(",").mapNotNull { it.trim().toDoubleOrNull() }
                if (coords.size >= 2) {
                    UserLocationManager.saveUserLocation(context, coords[0], coords[1], resolved)
                }

                savedStateHandle.remove<String>("selected_location")
            }
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onPostCreated()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create Post", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Post Type Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Post Type *",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = showPostTypeDropdown,
                        onExpandedChange = { showPostTypeDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = when(uiState.postType) {
                                PostType.ISSUE -> "Issue Report"
                                PostType.EVENT -> "Event"
                                PostType.POST -> "Social Post"
                            },
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Select Post Type") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = showPostTypeDropdown,
                            onDismissRequest = { showPostTypeDropdown = false }
                        ) {
                            PostType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(when(type) {
                                        PostType.ISSUE -> "Issue Report"
                                        PostType.EVENT -> "Event"
                                        PostType.POST -> "Social Post"
                                    }) },
                                    onClick = {
                                        viewModel.onPostTypeChange(type)
                                        showPostTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Category Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Category *",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = showCategoryDropdown,
                        onExpandedChange = { showCategoryDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.category,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Select Category") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false }
                        ) {
                            val categories = listOf("Health", "Roads", "Infrastructure", "Lost & Found", "Events", "Emergency", "General")
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        viewModel.onCategoryChange(category)
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Dynamic Content Card based on Post Type
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (uiState.postType) {
                        PostType.ISSUE -> {
                            Text(
                                text = "Issue Details *",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.title,
                                onValueChange = viewModel::onTitleChange,
                                label = { Text("Issue Title") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.description,
                                onValueChange = viewModel::onDescriptionChange,
                                label = { Text("Issue Description") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Priority Selection
                            ExposedDropdownMenuBox(
                                expanded = showPriorityDropdown,
                                onExpandedChange = { showPriorityDropdown = it }
                            ) {
                                OutlinedTextField(
                                    value = when(uiState.priority) {
                                        in 1..3 -> "Low"
                                        in 4..6 -> "Medium"
                                        in 7..8 -> "High"
                                        in 9..10 -> "Critical"
                                        else -> "Medium"
                                    },
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Priority") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )

                                ExposedDropdownMenu(
                                    expanded = showPriorityDropdown,
                                    onDismissRequest = { showPriorityDropdown = false }
                                ) {
                                    listOf(
                                        "Low" to 2,
                                        "Medium" to 5,
                                        "High" to 7,
                                        "Critical" to 9
                                    ).forEach { (label, value) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                viewModel.onPriorityChange(value)
                                                showPriorityDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Status Selection
                            ExposedDropdownMenuBox(
                                expanded = showStatusDropdown,
                                onExpandedChange = { showStatusDropdown = it }
                            ) {
                                OutlinedTextField(
                                    value = uiState.status ?: "Open",
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Status") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )

                                ExposedDropdownMenu(
                                    expanded = showStatusDropdown,
                                    onDismissRequest = { showStatusDropdown = false }
                                ) {
                                    listOf("Open", "Reported", "In Progress", "Resolved").forEach { status ->
                                        DropdownMenuItem(
                                            text = { Text(status) },
                                            onClick = {
                                                viewModel.onStatusChange(status)
                                                showStatusDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            Text(
                                text = "Content *",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.caption,
                                onValueChange = viewModel::onCaptionChange,
                                label = { Text("What's happening?") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 8
                            )
                        }
                    }
                }
            }

            // Location Card - Redesigned for GPS and Map only
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Display current selected location
                    if (uiState.location.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Selected Location:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = uiState.location,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.onLocationChange("")
                                    }
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Remove location")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Location action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val hasLocationPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasLocationPermission) {
                                    getUserCurrentLocation(context, viewModel, snackbarHostState, scope)
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Current Location")
                        }

                        OutlinedButton(
                            onClick = {
                                // Navigate to existing MapScreen with isPicker=true instead of map_picker
                                navController.navigate("map?isPicker=true")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Choose on Map")
                        }
                    }
                }
            }

            // Tags and Media Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Additional Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.tags.joinToString(", "),
                        onValueChange = viewModel::onTagsChange,
                        label = { Text("Tags (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. urgent, safety, community") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Post to local community only")
                        Switch(
                            checked = uiState.isLocalOnly,
                            onCheckedChange = viewModel::onLocalOnlyToggle
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Check runtime camera permission directly
                                val hasCameraPermission = PermissionUtils.hasCameraPermission(context)

                                if (hasCameraPermission) {
                                    // Permission already granted, create image file and launch camera
                                    try {
                                        val file = createImageFile(context)
                                        cameraImageFile.value = file
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                        cameraImageUri.value = uri
                                        cameraLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        scope.launch { snackbarHostState.showSnackbar("Failed to open camera: ${e.message}") }
                                    }
                                } else {
                                    // Request camera permission
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Camera",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gallery",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { videoLauncher.launch("video/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Video",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Show selected media (multiple images support)
                    if (uiState.imageUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${uiState.imageUris.size} image(s) selected", modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.onImageUrisChange(emptyList()) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear images")
                                }
                            }
                        }
                    }

                    if (uiState.videoUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Video selected", modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.onVideoUriChange(null) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                }
            }

            // Create Post Button
            Button(
                onClick = { viewModel.createPost(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                    Text("Create Post", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Suppress("DEPRECATION")
private fun getUserCurrentLocation(
    context: Context,
    viewModel: CreatePostViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    try {
        // Check if location services are enabled
        if (!PermissionUtils.isLocationServiceEnabled(context)) {
            scope.launch {
                snackbarHostState.showSnackbar("Please enable location services in settings")
            }
            return
        }

        // Check permissions
        if (!PermissionUtils.hasAnyLocationPermission(context)) {
            scope.launch {
                snackbarHostState.showSnackbar("Location permission required")
            }
            return
        }

        scope.launch {
            snackbarHostState.showSnackbar("Getting current location...")
        }

        // Get last known location using consolidated utility function
        val lastKnownLocation = PermissionUtils.getLastKnownLocation(context)

        if (lastKnownLocation != null) {
            // Reverse-geocode the coordinates to a human-readable address off the main thread
            scope.launch {
                val addressString = try {
                    androidGeocodeBestName(context, lastKnownLocation.latitude, lastKnownLocation.longitude)
                        ?: "${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}"
                 } catch (_: Exception) {
                     "${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}"
                 }

                 // Update the ViewModel and save location using the resolved address (or coordinates fallback)
                 viewModel.onLocationChange(addressString)

                 UserLocationManager.saveUserLocation(
                     context,
                     lastKnownLocation.latitude,
                     lastKnownLocation.longitude,
                     addressString
                 )

                 scope.launch {
                     snackbarHostState.showSnackbar("Location added successfully!")
                 }
             }
         } else {
             scope.launch {
                 snackbarHostState.showSnackbar("Unable to get current location. Please try again or use map.")
             }
         }
     } catch (e: Exception) {
         scope.launch {
             snackbarHostState.showSnackbar("Error getting location: ${e.message}")
         }
     }
 }

@Throws(Exception::class)
private fun createImageFile(context: Context): File {
    // Create an image file name with timestamp
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"

    // Use cache directory instead of external files for better compatibility
    val storageDir: File = context.cacheDir

    return File.createTempFile(
        imageFileName, /* prefix */
        ".jpg",         /* suffix */
        storageDir      /* directory */
    )
}

// Inline helper to build plain address from Android Geocoder
private suspend fun androidGeocodeBestName(context: Context, lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
    try {
        @Suppress("DEPRECATION")
        val list: List<Address>? = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)
        list?.firstOrNull()?.getAddressLine(0)
    } catch (_: Exception) { null }
}
