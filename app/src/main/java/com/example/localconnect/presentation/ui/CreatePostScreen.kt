package com.example.localconnect.presentation.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.LocationManager
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
import kotlinx.coroutines.launch
import android.content.Context

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

    // Media launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        scope.launch {
            snackbarHostState.showSnackbar("Photo captured! (Bitmap to URI conversion needed)")
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onImageUriChange(uri)
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
            cameraLauncher.launch(null)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission denied")
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

    // Handle navigation result for location
    LaunchedEffect(Unit) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        savedStateHandle?.get<String>("selected_location")?.let { locationString ->
            viewModel.onLocationChange(locationString)
            savedStateHandle.remove<String>("selected_location")
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
                                val hasCameraPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasCameraPermission) {
                                    cameraLauncher.launch(null)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Camera")
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery")
                        }

                        OutlinedButton(
                            onClick = { videoLauncher.launch("video/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Video")
                        }
                    }

                    // Show selected media
                    if (uiState.imageUri != null) {
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
                                Text("Image selected", modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.onImageUriChange(null) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove")
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
                onClick = { viewModel.createPost() },
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
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Check if location services are enabled
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            scope.launch {
                snackbarHostState.showSnackbar("Please enable location services in settings")
            }
            return
        }

        // Check permissions
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            scope.launch {
                snackbarHostState.showSnackbar("Location permission required")
            }
            return
        }

        scope.launch {
            snackbarHostState.showSnackbar("Getting current location...")
        }

        // Get last known location first (faster)
        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (lastKnownLocation != null) {
            val locationString = "${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}"
            viewModel.onLocationChange(locationString)

            // Save to user preferences for HomeViewModel location filtering
            UserLocationManager.saveUserLocation(
                context,
                lastKnownLocation.latitude,
                lastKnownLocation.longitude,
                locationString
            )

            scope.launch {
                snackbarHostState.showSnackbar("Location added successfully!")
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
