package com.example.darts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.darts.ui.screens.BattlesScreen
import com.example.darts.ui.screens.GameImportScreen
import com.example.darts.ui.screens.GameScreen
import com.example.darts.ui.screens.GameSettingsScreen
import com.example.darts.ui.screens.GameSharingScreen
import com.example.darts.ui.screens.HomeScreen
import com.example.darts.ui.screens.LegSummaryScreen
import com.example.darts.ui.screens.MapScreen
import com.example.darts.ui.screens.MatchSummaryScreen
import com.example.darts.ui.screens.MomentsGalleryScreen
import com.example.darts.ui.screens.PlayerStatsScreen
import com.example.darts.ui.screens.PlayersScreen
import com.example.darts.ui.screens.SettingsScreen
import com.example.darts.ui.screens.StatisticsOverviewScreen
import com.example.darts.ui.screens.TurnHistoryScreen
import com.example.darts.ui.screens.score_entry.BoardButtonsScreen
import com.example.darts.ui.screens.score_entry.CameraScanScreen
import com.example.darts.ui.screens.score_entry.TypeAndEnterScreen
import com.example.darts.ui.screens.score_entry.VoiceRecognitionScreen
import com.example.darts.ui.theme.DartsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DartsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        GreetingPreview()
                    }
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SettingsScreen()
}