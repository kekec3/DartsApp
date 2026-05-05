package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsGalleryScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moments", fontWeight = FontWeight.Bold) },
                actions = {
                    Surface(
                        onClick = { },
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("All", fontSize = 12.sp, color = Color.White)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = { DartsBottomBar() }
    ) { innerPadding ->
        val moments = listOf(
            MomentItem("T20 Checkout", "May 18, 2024"),
            MomentItem("180!", "May 15, 2024"),
            MomentItem("Bullseye", "May 10, 2024"),
            MomentItem("140+", "May 8, 2024")
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            items(moments) { moment ->
                MomentCard(moment)
            }
        }
    }
}

@Composable
fun MomentCard(moment: MomentItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f), // Taller cards like the design
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Placeholder for the image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray) // Replace with Image() and ContentScale.Crop
            )

            // Text Overlay at the bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(12.dp)
            ) {
                Text(
                    text = moment.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = moment.date,
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

data class MomentItem(val title: String, val date: String)