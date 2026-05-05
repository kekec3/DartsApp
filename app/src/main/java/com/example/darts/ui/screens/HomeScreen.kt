package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darts.R

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
    val icon: Painter
)

// -------------------- SCREEN --------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val items = listOf(
        MenuItemData("Battles", "View your battles", AppIcons.Battles),
        MenuItemData("Players", "Manage players", AppIcons.Players),
        MenuItemData("Statistics", "View stats & insights", AppIcons.Statistics),
        MenuItemData("Moments", "Your best moments", AppIcons.Moments),
        MenuItemData("Map", "Where you've played", AppIcons.Map)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DART SCORE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            Spacer(modifier = Modifier.height(8.dp))

            PrimaryActionButton(
                text = "Start Game",
                icon = Icons.Default.PlayArrow
            )

            Spacer(modifier = Modifier.height(16.dp))

            items.forEach {
                MenuItem(it.title, it.subtitle, it.icon)
            }
        }
    }
}

// -------------------- COMPONENTS --------------------

@Composable
fun PrimaryActionButton(
    text: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable { }
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Play a new game",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun MenuItem(
    title: String,
    subtitle: String,
    icon: Painter
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { }
            .padding(horizontal = 18.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp)
        )

        Spacer(modifier = Modifier.width(18.dp))

        Column {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                fontSize = 14.sp
            )
        }
    }
}