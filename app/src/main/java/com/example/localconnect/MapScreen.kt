package com.example.localconnect

import android.content.Context
import android.location.LocationManager
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var myLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var isLocationEnabled by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }

    // Permission launcher for map-specific permissions
    val mapPermissionLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        PermissionUtils.handleMapPermissionResult(context, permissions)
        val locationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false ||
                             permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (locationGranted) {
            // Re-initialize location overlay after permission granted
            mapView?.let { map ->
                initializeLocationOverlay(context, map) { overlay ->
                    myLocationOverlay = overlay
                    isLocationEnabled = true
                }
            }
            Toast.makeText(context, "Location permission granted! GPS enabled.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Location permission denied. GPS features disabled.", Toast.LENGTH_SHORT).show()
        }
    }

    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName

        // Request map permissions if not already granted
        if (!PermissionUtils.hasMapPermissions(context)) {
            mapPermissionLauncher.launch(PermissionUtils.getMapPermissions())
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Select Location") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Map View with floating GPS button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            OSMMap(
                modifier = Modifier.fillMaxSize(),
                onLocationSelected = { lat, lng ->
                    selectedLocation = GeoPoint(lat, lng)
                },
                onMapViewCreated = { createdMapView, createdMyLocationOverlay ->
                    mapView = createdMapView
                    myLocationOverlay = createdMyLocationOverlay
                    isLocationEnabled = createdMyLocationOverlay != null
                },
                onLocationUpdate = { location ->
                    currentLocation = location
                }
            )

            // Floating GPS button (bottom right like Google Maps)
            FloatingActionButton(
                onClick = {
                    when {
                        !PermissionUtils.hasMapPermissions(context) -> {
                            mapPermissionLauncher.launch(PermissionUtils.getMapPermissions())
                        }
                        !isLocationServiceEnabled(context) -> {
                            Toast.makeText(context, "Please enable GPS/Location services", Toast.LENGTH_LONG).show()
                        }
                        myLocationOverlay == null -> {
                            mapView?.let { map ->
                                initializeLocationOverlay(context, map) { overlay ->
                                    myLocationOverlay = overlay
                                    isLocationEnabled = true
                                }
                            }
                        }
                        else -> {
                            // Enable location and animate to current position
                            myLocationOverlay?.let { overlay ->
                                overlay.enableMyLocation()
                                overlay.enableFollowLocation()

                                // Try to get current location and animate to it
                                overlay.myLocation?.let { location ->
                                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                                    mapView?.controller?.animateTo(geoPoint)
                                    currentLocation = geoPoint
                                    Toast.makeText(context, "Centered on your location", Toast.LENGTH_SHORT).show()
                                } ?: run {
                                    // If no location yet, try to get it
                                    overlay.runOnFirstFix {
                                        overlay.myLocation?.let { location ->
                                            val geoPoint = GeoPoint(location.latitude, location.longitude)
                                            mapView?.post {
                                                mapView?.controller?.animateTo(geoPoint)
                                                currentLocation = geoPoint
                                            }
                                        }
                                    }
                                    Toast.makeText(context, "Getting your location...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(56.dp)
                    .shadow(8.dp, CircleShape),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    tint = if (isLocationEnabled && currentLocation != null)
                           MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Selected location info card
            selectedLocation?.let { location ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 80.dp), // Extra padding to avoid GPS button
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Selected Location",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Latitude: ${String.format("%.6f", location.latitude)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Longitude: ${String.format("%.6f", location.longitude)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // TODO: Save location and navigate back
                                    navController.popBackStack()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Use This Location")
                            }

                            OutlinedButton(
                                onClick = {
                                    selectedLocation = null
                                    // Clear map markers (except location overlay)
                                    mapView?.overlays?.removeIf { it is Marker }
                                    mapView?.invalidate()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to check if location services are enabled
private fun isLocationServiceEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
           locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

// Helper function to initialize location overlay
private fun initializeLocationOverlay(
    context: Context,
    mapView: MapView,
    onOverlayCreated: (MyLocationNewOverlay) -> Unit
) {
    if (PermissionUtils.hasMapPermissions(context)) {
        // Create a more accurate location provider
        val locationProvider = GpsMyLocationProvider(context).apply {
            // Set location update criteria for better accuracy
            locationUpdateMinTime = 1000L // Update every 1 second
            locationUpdateMinDistance = 1.0f // Update every 1 meter
        }

        val myLocationOverlay = MyLocationNewOverlay(locationProvider, mapView).apply {
            enableMyLocation()
            disableFollowLocation() // Don't follow initially

            // Wait for location fix before following
            runOnFirstFix {
                myLocation?.let { location ->
                    mapView.post {
                        enableFollowLocation()
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        mapView.controller.animateTo(geoPoint)
                    }
                }
            }
        }

        // Remove any existing location overlay
        mapView.overlays.removeIf { it is MyLocationNewOverlay }
        mapView.overlays.add(myLocationOverlay)
        mapView.invalidate()

        onOverlayCreated(myLocationOverlay)
    }
}

@Composable
fun OSMMap(
    modifier: Modifier = Modifier,
    onLocationSelected: (Double, Double) -> Unit,
    onMapViewCreated: (MapView, MyLocationNewOverlay?) -> Unit = { _, _ -> },
    onLocationUpdate: (GeoPoint) -> Unit = {}
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)

                // Default location: Bangalore
                val defaultLocation = GeoPoint(12.9716, 77.5946)
                controller.setCenter(defaultLocation)

                // Don't add default marker - let only the blue location dot be visible initially

                // Add My Location overlay if permission is available
                var myLocationOverlay: MyLocationNewOverlay? = null
                if (PermissionUtils.hasMapPermissions(context)) {
                    // Use enhanced location provider for better accuracy
                    val locationProvider = GpsMyLocationProvider(context).apply {
                        locationUpdateMinTime = 1000L // Update every 1 second
                        locationUpdateMinDistance = 1.0f // Update every 1 meter
                    }

                    val mapViewRef = this // Store reference to MapView
                    myLocationOverlay = MyLocationNewOverlay(locationProvider, this).apply {
                        enableMyLocation()
                        disableFollowLocation() // Don't follow initially

                        // Listen for accurate location updates
                        runOnFirstFix {
                            myLocation?.let { location ->
                                val geoPoint = GeoPoint(location.latitude, location.longitude)
                                mapViewRef.post {
                                    controller.animateTo(geoPoint)
                                    onLocationUpdate(geoPoint)
                                    enableFollowLocation() // Enable following after accurate fix
                                }
                            }
                        }
                    }
                    overlays.add(myLocationOverlay)
                }

                // Add tap listener for location selection
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        val proj = projection
                        val geoPoint = proj.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint

                        // Remove existing selection markers (keep location overlay)
                        overlays.removeIf { it is Marker }

                        // Add new green marker at tapped location
                        val marker = Marker(this).apply {
                            position = geoPoint
                            title = "Selected Location"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            // Set marker color to green to distinguish from blue location dot
                        }
                        overlays.add(marker)

                        // Re-add MyLocation overlay if it exists to keep blue dot on top
                        myLocationOverlay?.let {
                            overlays.remove(it)
                            overlays.add(it)
                        }

                        invalidate()
                        onLocationSelected(geoPoint.latitude, geoPoint.longitude)
                    }
                    false
                }

                // Notify about map view creation
                onMapViewCreated(this, myLocationOverlay)
            }
        }
    )
}
