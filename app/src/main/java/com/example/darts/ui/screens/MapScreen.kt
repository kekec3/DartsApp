package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*

@Composable
fun MapScreen(
    modifier: Modifier = Modifier
) {
    // Coordinates for the School of Electrical Engineering (ETF)
    val etfBelgrade = LatLng(44.8061, 20.4761)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(etfBelgrade, 15f)
    }

    // Standard Dark Mode JSON for Google Maps
    val darkMapStyle = """
        [
          { "elementType": "geometry", "stylers": [{ "color": "#242f3e" }] },
          { "elementType": "labels.text.stroke", "stylers": [{ "color": "#242f3e" }] },
          { "elementType": "labels.text.fill", "stylers": [{ "color": "#746855" }] },
          { "featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{ "color": "#d59563" }] },
          { "featureType": "poi", "elementType": "labels.text.fill", "stylers": [{ "color": "#d59563" }] },
          { "featureType": "poi.park", "elementType": "geometry", "stylers": [{ "color": "#263c3f" }] },
          { "featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{ "color": "#6b9a76" }] },
          { "featureType": "road", "elementType": "geometry", "stylers": [{ "color": "#38414e" }] },
          { "featureType": "road", "elementType": "geometry.stroke", "stylers": [{ "color": "#212a37" }] },
          { "featureType": "road", "elementType": "labels.text.fill", "stylers": [{ "color": "#9ca5b3" }] },
          { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#17263c" }] },
          { "featureType": "water", "elementType": "labels.text.fill", "stylers": [{ "color": "#515c6d" }] }
        ]
    """.trimIndent()

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = MapStyleOptions(darkMapStyle)
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        ) {
            Marker(
                state = rememberMarkerState(position = etfBelgrade),
                title = "Match at ETF",
                snippet = "John vs Mike"
            )
        }

        // Top Search Bar (Floating)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Surface(
                onClick = { /* Open Search */ },
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Match Info Card Overlay
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E).copy(alpha = 0.9f)
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("John vs Mike", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("May 20, 2024", color = Color.Gray, fontSize = 12.sp)
                    Text("Bulevar kralja Aleksandra 73", color = Color(0xFF76B947), fontSize = 14.sp)
                }
                Text("3 - 1", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}