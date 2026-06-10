package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.darts.viewModel.PlayerStatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerStatsScreen(
    modifier: Modifier = Modifier,
    playerId: Int,
    playerName: String,
    onBack: () -> Unit = {},
    viewModel: PlayerStatsViewModel =  hiltViewModel()
) {

    LaunchedEffect(playerId) {
        viewModel.load(playerId)
    }

    val career by viewModel.careerStats.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(playerName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                // Spacer to keep title centered
                Box(modifier = Modifier.size(48.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        // Tabs Section
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf("Overview")

        val stats = career

        val average =
            if ((stats?.totalDartsThrown ?: 0) > 0)
                stats!!.totalScored.toFloat() /
                        stats.totalDartsThrown * 3
            else 0f

        val winRate =
            if ((stats?.matchesPlayed ?: 0) > 0)
                stats!!.matchesWon * 100f /
                        stats.matchesPlayed
            else 0f

        val checkoutPct =
            if ((stats?.checkoutsAttempted ?: 0) > 0)
                stats!!.checkoutsHit * 100f /
                        stats.checkoutsAttempted
            else 0f


        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Black,
            contentColor = Color(0xFF76B947),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF76B947)
                )
            },
            divider = { HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) Color.White else Color.Gray,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Time Filter
            Surface(
                onClick = { /* Filter */ },
                color = Color.Transparent
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("All Time", color = Color(0xFF76B947), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF76B947))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val overview = listOf(
                        "Matches Played" to
                                "${stats?.matchesPlayed ?: 0}",

                        "Matches Won" to
                                "${stats?.matchesWon ?: 0}",

                        "Win Rate" to
                                "%.1f%%".format(winRate),

                        "3-Dart Average" to
                                "%.2f".format(average),

                        "Checkout %" to
                                "%.1f%%".format(checkoutPct),

                        "Highest Checkout" to
                                "${stats?.highestCheckout ?: 0}",

                        "180s" to
                                "${stats?.scores180 ?: 0}",

                        "140+ Scores" to
                                "${stats?.scores140Plus ?: 0}",

                        "100+ Scores" to
                                "${stats?.scores100Plus ?: 0}",

                        "Legs Played" to
                                "${stats?.legsPlayed ?: 0}",

                        "Legs Won" to
                                "${stats?.legsWon ?: 0}"
                    )

                    overview.forEachIndexed { index, (label, value) ->
                        PlayerStatItem(label, value)

                        if (index < overview.lastIndex) {
                            HorizontalDivider(
                                color = Color.DarkGray.copy(alpha = 0.3f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerStatItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 15.sp)
        Text(value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}