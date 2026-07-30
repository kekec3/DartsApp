package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darts.R
import com.example.darts.utils.DartShape

// -------------------- ICONS --------------------

object AppIcons {
    val Battles @Composable get() = painterResource(R.drawable.battle_icon)
    val Players @Composable get() = painterResource(R.drawable.players_icon)
    val Statistics @Composable get() = painterResource(R.drawable.statistics_icon)
    val Moments @Composable get() = painterResource(R.drawable.top_moments_icon)
    val Map @Composable get() = painterResource(R.drawable.map_icon)
}

// -------------------- DATA --------------------

data class MenuItemData(
    val title: String,
    val subtitle: String,
    val icon: Painter,
    val onClick: () -> Unit
)

// -------------------- SCREEN --------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onStartGame: () -> Unit = {},
    onViewBattles: () -> Unit = {},
    onViewPlayers: () -> Unit = {},
    onViewStats: () -> Unit = {},
    onViewMoments: () -> Unit = {},
    onViewMap: () -> Unit = {},
    onViewSettings: () -> Unit = {}
) {
    val menuItems = listOf(
        MenuItemData("Battles", "Review your match history", AppIcons.Battles, onViewBattles),
        MenuItemData("Players", "Manage friends and rivals", AppIcons.Players, onViewPlayers),
        MenuItemData("Statistics", "Performance & trends", AppIcons.Statistics, onViewStats),
        MenuItemData("Moments", "Captured highlights", AppIcons.Moments, onViewMoments),
        MenuItemData("Map", "Nearby dart boards", AppIcons.Map, onViewMap)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "DART SCORE",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color.White
                )
            },
            actions = {
                IconButton(onClick = onViewSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            PrimaryActionButton(
                text = "START GAME",
                subtitle = "New 501 or Cricket match",
                onClick = onStartGame
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MAIN MENU",
                color = Color(0xFF76B947),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            menuItems.forEach { item ->
                MenuItem(item.title, item.subtitle, item.icon, item.onClick)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// -------------------- COMPONENTS --------------------

@Composable
fun PrimaryActionButton(
    text: String = "START GAME",
    subtitle: String = "New 501 or Cricket match",
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF8CDB00),
                        Color(0xFF5CA300)
                    )
                )
            )
            .clickable { onClick() }
    ) {
        // Watermarked Dart XML bleeding off the right edge
        Icon(
            painter = painterResource(id = R.drawable.ic_dart),
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.14f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 16.dp, y = 4.dp)
                .size(130.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High-contrast dark play badge
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xFF0B0F0C), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF8CDB00),
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    color = Color(0xFF0B0F0C),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF0B0F0C).copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MenuItem(
    title: String,
    subtitle: String,
    icon: Painter,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A1A))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFF252525), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = title,
                tint = Color(0xFF76B947),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(18.dp))

        Column {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}