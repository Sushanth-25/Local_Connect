package com.example.localconnect

import android.content.Context
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.localconnect.util.PermissionUtils
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.*

@Composable
fun rememberMapView(context: Context): MapView {
    return remember { MapView(context) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isPicker: Boolean = false
) {
    val context = LocalContext.current
    val mapView = rememberMapView(context)
    var myLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Permission launcher
    val mapPermissionLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        PermissionUtils.handleMapPermissionResult(context, permissions)
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            initializeLocationOverlay(context, mapView) { overlay ->
                myLocationOverlay = overlay
            }
        } else {
            Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    // OSMDroid setup
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName

        if (!PermissionUtils.hasMapPermissions(context)) {
            mapPermissionLauncher.launch(PermissionUtils.getMapPermissions())
        } else {
            initializeLocationOverlay(context, mapView) { overlay ->
                myLocationOverlay = overlay
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Nearby Map (3 km radius)") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            ) { mv ->
                mv.setTileSource(TileSourceFactory.MAPNIK)
                mv.setMultiTouchControls(true)
                mv.controller.setZoom(15.0)

                // Center on Bengaluru temporarily until location fix
                mv.controller.setCenter(GeoPoint(12.9716, 77.5946))

                if (PermissionUtils.hasMapPermissions(context)) {
                    initializeLocationOverlay(context, mv) { overlay ->
                        myLocationOverlay = overlay
                    }
                }
            }

            // Floating button for location
            FloatingActionButton(
                onClick = {
                    if (!PermissionUtils.hasMapPermissions(context)) {
                        mapPermissionLauncher.launch(PermissionUtils.getMapPermissions())
                    } else {
                        myLocationOverlay?.enableMyLocation()
                        myLocationOverlay?.runOnFirstFix {
                            val loc = myLocationOverlay?.myLocation
                            if (loc != null) {
                                val geo = GeoPoint(loc.latitude, loc.longitude)
                                mapView.post {
                                    mapView.controller.animateTo(geo)
                                    showRadius(mapView, geo)
                                    Toast.makeText(context, "Showing 3 km radius", Toast.LENGTH_SHORT).show()
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
                    contentDescription = "My Location"
                )
            }
        }
    }
}

// --- Helper: Draw a 3 km radius circle ---
private fun showRadius(mapView: MapView, center: GeoPoint) {
    // Remove old circles
    mapView.overlays.removeIf { it is Polygon }

    val radiusMeters = 3000.0
    val earthRadius = 6378137.0
    val lat = Math.toRadians(center.latitude)

    val points = mutableListOf<GeoPoint>()
    for (i in 0..360 step 5) {
        val angle = Math.toRadians(i.toDouble())
        val dx = radiusMeters * cos(angle) / earthRadius
        val dy = radiusMeters * sin(angle) / (earthRadius * cos(lat))
        val latPoint = center.latitude + Math.toDegrees(dy)
        val lonPoint = center.longitude + Math.toDegrees(dx)
        points.add(GeoPoint(latPoint, lonPoint))
    }

    val circle = Polygon(mapView).apply {
        outlinePaint.color = android.graphics.Color.BLUE
        outlinePaint.strokeWidth = 4f
        fillPaint.color = android.graphics.Color.argb(40, 0, 0, 255)
        this.points.addAll(points)
    }

    mapView.overlays.add(circle)
    mapView.invalidate()
}

// --- Location overlay setup ---
private fun initializeLocationOverlay(
    context: Context,
    mapView: MapView,
    onOverlayCreated: (MyLocationNewOverlay) -> Unit
) {
    val locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = GpsMyLocationProvider(context).apply {
        locationUpdateMinTime = 1000L
        locationUpdateMinDistance = 1f
        addLocationSource(LocationManager.NETWORK_PROVIDER)
    }

    val overlay = MyLocationNewOverlay(provider, mapView).apply {
        enableMyLocation()
        disableFollowLocation()
    }

    // ✅ Step 1: Try to use last known or emulator location immediately
    try {
        val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        val initialGeo = if (lastKnown != null) {
            GeoPoint(lastKnown.latitude, lastKnown.longitude)
        } else {
            // fallback: Bengaluru center
            GeoPoint(12.9716, 77.5946)
        }

        mapView.post {
            mapView.controller.setZoom(16.5)
            mapView.controller.setCenter(initialGeo)
            showRadius(mapView, initialGeo) // ✅ Draw 3 km radius immediately
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    }

    // ✅ Step 2: Once GPS fix arrives, re-center and update circle
    overlay.runOnFirstFix {
        val loc = overlay.myLocation
        if (loc != null) {
            val geo = GeoPoint(loc.latitude, loc.longitude)
            mapView.post {
                mapView.controller.animateTo(geo)
                showRadius(mapView, geo)
                Toast.makeText(context, "Updated to your current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    mapView.overlays.removeIf { it is MyLocationNewOverlay }
    mapView.overlays.add(overlay)
    mapView.invalidate()
    onOverlayCreated(overlay)
}
