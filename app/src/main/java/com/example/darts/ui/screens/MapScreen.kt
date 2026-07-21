package com.example.darts.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // 👈 DODOAT IMPORT
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darts.db.entities.Game
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer // 👈 DODAT IMPORT
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlin.math.abs

// ── Lista dostupnih boja za markere ──────────────────────────────────────────
private val markerHues = listOf(
    BitmapDescriptorFactory.HUE_RED,
    BitmapDescriptorFactory.HUE_BLUE,
    BitmapDescriptorFactory.HUE_GREEN,
    BitmapDescriptorFactory.HUE_YELLOW,
    BitmapDescriptorFactory.HUE_VIOLET,
    BitmapDescriptorFactory.HUE_ORANGE,
    BitmapDescriptorFactory.HUE_CYAN,
    BitmapDescriptorFactory.HUE_ROSE,
    BitmapDescriptorFactory.HUE_AZURE,
    BitmapDescriptorFactory.HUE_MAGENTA
)

private fun getHueForBattle(battleId: Int?): Float {
    if (battleId == null) return BitmapDescriptorFactory.HUE_GREEN
    val safeIndex = abs(battleId) % markerHues.size
    return markerHues[safeIndex]
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun MapScreen(
    games: List<Game>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 0. Eksplicitna inicijalizacija Maps SDK-a kako CameraUpdateFactory ne bi bio null
    LaunchedEffect(Unit) {
        MapsInitializer.initialize(context)
    }

    // 1. Bezbedno konvertovanje lokacija
    val gamePoints = remember(games) {
        games.mapNotNull { game ->
            val rawLocation = game.location ?: return@mapNotNull null
            val parts = rawLocation.split(",")
            if (parts.size == 2) {
                val lat = parts[0].trim().toDoubleOrNull()
                val lng = parts[1].trim().toDoubleOrNull()
                if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
                    LatLng(lat, lng) to game
                } else null
            } else null
        }
    }

    // 2. Pamti izabranu igru
    var selectedGame by remember { mutableStateOf<Game?>(null) }

    LaunchedEffect(gamePoints) {
        if (selectedGame == null && gamePoints.isNotEmpty()) {
            selectedGame = gamePoints.first().second
        }
    }

    // 3. Početna pozicija kamere
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(44.8061, 20.4761), 12f)
    }

    // Animira kameru na prvu igru čim se podaci učitaju
    LaunchedEffect(gamePoints) {
        if (gamePoints.isNotEmpty()) {
            // Osiguravamo inicijalizaciju pre poziva CameraUpdateFactory
            MapsInitializer.initialize(context)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(gamePoints.first().first, 12f)
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = MapStyleOptions(DARK_MAP_JSON)
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = true
            ),
            onMapClick = { selectedGame = null }
        ) {
            gamePoints.forEach { (point, game) ->
                val markerColor = getHueForBattle(game.idBattle)

                Marker(
                    state = MarkerState(position = point),
                    title = "Game #${game.idGame}",
                    icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                    onClick = {
                        selectedGame = game
                        false
                    }
                )
            }
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
        selectedGame?.let { game ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
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
                        Text(
                            text = "Game #${game.idGame}",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = game.date,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Coords: ${game.location}",
                            color = Color(0xFF76B947),
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = game.type,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

const val DARK_MAP_JSON = """
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
"""