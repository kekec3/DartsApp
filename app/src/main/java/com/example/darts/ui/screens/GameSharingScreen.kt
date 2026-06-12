package com.example.darts.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darts.db.entities.Player
import com.example.darts.ui.viewmodels.GameSharingViewModel
import com.example.darts.ui.viewmodels.ShareUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSharingScreen(
    modifier: Modifier = Modifier,
    viewModel: GameSharingViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val players by viewModel.players.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Helper launcher function to trigger native share actions
    val launchShareIntent = { uri: android.net.Uri, text: String, targetPackage: String? ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream" // Matches file type pattern
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            targetPackage?.let { setPackage(it) }
        }
        context.startActivity(Intent.createChooser(intent, "Share Darts History"))
    }

    // Observe sharing success triggers
    LaunchedEffect(uiState) {
        if (uiState is ShareUiState.Success) {
            val successState = uiState as ShareUiState.Success
            // Trigger share and clear state
            launchShareIntent(successState.fileUri, successState.shareText, null)
            viewModel.resetUiState()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Export Player History", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Player Selection Box Dropdown
            Text("Select Player to Export", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                    .clickable { isDropdownExpanded = true }
                    .padding(16.dp)
            ) {
                Text(
                    text = selectedPlayer?.username ?: "Tap to choose a player...",
                    color = if (selectedPlayer != null) Color.White else Color.DarkGray,
                    fontSize = 16.sp
                )

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.background(Color(0xFF1A1A1A))
                ) {
                    players.forEach { player ->
                        DropdownMenuItem(
                            text = { Text(player.username, color = Color.White) },
                            onClick = {
                                selectedPlayer = player
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Match Summary Card Preview
            selectedPlayer?.let { player ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(player.username, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Ready to package all associated battles & metrics", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(".darts Export File", color = Color(0xFF76B947), fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Share Actions Panel
            Text("Share via Link & File", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShareOptionItem("WhatsApp", Icons.Default.Send, Color(0xFF25D366)) {
                    selectedPlayer?.let { viewModel.prepareExport(context, it) }
                }
                ShareOptionItem("Messages", Icons.Default.Share, Color(0xFF007AFF)) {
                    selectedPlayer?.let { viewModel.prepareExport(context, it) }
                }
                ShareOptionItem("Gmail", Icons.Default.Email, Color(0xFFEA4335)) {
                    selectedPlayer?.let { viewModel.prepareExport(context, it) }
                }
                ShareOptionItem("More", Icons.Default.MoreVert, Color(0xFF1E1E1E)) {
                    selectedPlayer?.let { viewModel.prepareExport(context, it) }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState is ShareUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFF76B947))
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Export Confirmation Button
            Button(
                onClick = { selectedPlayer?.let { viewModel.prepareExport(context, it) } },
                enabled = selectedPlayer != null && uiState !is ShareUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF76B947),
                    disabledContainerColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "PREPARE AND SHARE ALL DATA",
                    color = if (selectedPlayer != null) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun ShareOptionItem(label: String, icon: ImageVector, bgColor: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}