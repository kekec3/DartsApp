package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.darts.db.entities.Moment
import com.example.darts.db.repositories.MomentRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File

// Interface lookup engine allows repository injection without a ViewModel wrapper
@EntryPoint
@InstallIn(SingletonComponent::class)
interface GalleryEntryPoint {
    fun momentRepository(): MomentRepository
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsGalleryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext

    // Dynamically resolve repository via Hilt Context EntryPoint
    val repository = remember {
        EntryPointAccessors.fromApplication(context, GalleryEntryPoint::class.java).momentRepository()
    }

    // Reactively collect state directly from the local database layer
    val moments by repository.getPhotos().collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = { Text("Match Photos", fontWeight = FontWeight.Bold, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        if (moments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No photos captured for this match yet.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(moments) { moment ->
                    MomentCard(moment)
                }
            }
        }
    }
}

@Composable
fun MomentCard(moment: Moment) {
    val context = LocalContext.current

    // Dynamically resolve where this file actually lives
    val imageModel = remember(moment.contentValue) {
        val path = moment.contentValue
        when {
            // Case 1: It's already a full URI path (content:// or file://)
            path.startsWith("content://") || path.startsWith("file://") -> path

            else -> {
                val absoluteFile = File(path)
                if (absoluteFile.exists()) {
                    // Case 2: It's an absolute path that works out of the box
                    absoluteFile
                } else {
                    // Case 3: It's just a filename, check the app's internal storage files directory
                    val internalFile = File(context.filesDir, path)
                    if (internalFile.exists()) {
                        internalFile
                    } else {
                        // Case 4: Check the app's cache directory just in case
                        val cacheFile = File(context.cacheDir, path)
                        if (cacheFile.exists()) {
                            cacheFile
                        } else {
                            null // Truly missing from disk
                        }
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Captured Moment",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback state with a debug label showing what string is breaking it
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2A2A2A))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Image not found", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "DB Value: ${moment.contentValue}",
                            color = Color.DarkGray,
                            fontSize = 10.sp,
                            maxLines = 2
                        )
                    }
                }
            }

            // Bottom Info Details Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Game #${moment.idGame}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}