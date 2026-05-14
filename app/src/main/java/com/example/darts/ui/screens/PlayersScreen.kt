package com.example.darts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.darts.db.entities.Battle
import com.example.darts.db.entities.Player
import com.example.darts.viewModel.BattleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    modifier: Modifier = Modifier,
    battleViewModel: BattleViewModel = hiltViewModel(),
    onBattleCreated: (Int) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var showBattleNameDialog by remember { mutableStateOf(false) }
    var duplicateBattle by remember { mutableStateOf<Battle?>(null) }

    val players by battleViewModel.availablePlayers.collectAsState()
    val selectedIds by battleViewModel.selectedPlayerIds.collectAsState()
    val battleName by battleViewModel.battleName.collectAsState()

    // 1. ADD PLAYER DIALOG
    if (showAddPlayerDialog) {
        AddPlayerDialog(
            onDismiss = { showAddPlayerDialog = false },
            onConfirm = { name, avatar ->
                battleViewModel.addNewPlayer(name, avatar)
                showAddPlayerDialog = false
            }
        )
    }

    // 2. FINALIZE BATTLE DIALOG
    if (showBattleNameDialog) {
        AlertDialog(
            onDismissRequest = { showBattleNameDialog = false },
            title = { Text("Start Battle", fontWeight = FontWeight.Bold, color = Color.White) },
            containerColor = Color(0xFF1E1E1E),
            text = {
                OutlinedTextField(
                    value = battleName,
                    onValueChange = { battleViewModel.onNameChange(it) },
                    label = { Text("Battle Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF76B947),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF76B947)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        battleViewModel.saveBattle(
                            onSuccess = { newId ->
                                showBattleNameDialog = false
                                onBattleCreated(newId)
                            },
                            onDuplicateFound = { existing ->
                                showBattleNameDialog = false
                                duplicateBattle = existing
                            }
                        )
                    },
                    enabled = battleName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76B947))
                ) {
                    Text("Ready", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBattleNameDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // 3. DUPLICATE BATTLE RESOLUTION
    if (duplicateBattle != null) {
        AlertDialog(
            onDismissRequest = { duplicateBattle = null },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Battle Found", color = Color.White) },
            text = {
                Text(
                    "A battle named '${duplicateBattle?.name}' already exists with these players. Use existing or create new?",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = duplicateBattle?.idBattle
                        duplicateBattle = null
                        onBattleCreated(id!!)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76B947))
                ) { Text("Use Existing", color = Color.Black) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        battleViewModel.createNewBattle { idNew ->
                            duplicateBattle = null
                            onBattleCreated(idNew)
                        }
                    }
                ) { Text("Create New", color = Color(0xFF76B947)) }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Players", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = if (selectedIds.size < 2) "Select 2-4" else "${selectedIds.size}/4 Selected",
                        fontSize = 12.sp,
                        color = if (selectedIds.size in 2..4) Color(0xFF76B947) else Color.Gray
                    )
                }
            },
            actions = {
                IconButton(onClick = { showAddPlayerDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF76B947))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    placeholder = { Text("Search Players...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF76B947),
                        unfocusedBorderColor = Color.DarkGray,
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedContainerColor = Color(0xFF1A1A1A)
                    )
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(
                        items = players.filter { it.username.contains(searchQuery, ignoreCase = true) },
                        key = { it.idPlayer }
                    ) { player ->
                        PlayerListItem(
                            player = player,
                            isSelected = selectedIds.contains(player.idPlayer),
                            onSelect = {
                                if (selectedIds.contains(player.idPlayer) || selectedIds.size < 4) {
                                    battleViewModel.togglePlayer(player.idPlayer)
                                }
                            }
                        )
                    }
                }
            }

            if (selectedIds.size in 2..4) {
                ExtendedFloatingActionButton(
                    onClick = { showBattleNameDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    containerColor = Color(0xFF76B947),
                    contentColor = Color.Black,
                    text = { Text("CONFIRM PLAYERS", fontWeight = FontWeight.ExtraBold) },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
fun AddPlayerDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("🎯") }
    val presets = listOf("🎯", "🔥", "🎲", "👤", "⚡", "🏆")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("New Player", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF76B947),
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Text("Select Avatar", color = Color.Gray, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presets.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (selectedAvatar == emoji) Color(0xFF76B947) else Color(0xFF2A2A2A))
                                .clickable { selectedAvatar = emoji },
                            contentAlignment = Alignment.Center
                        ) { Text(emoji, fontSize = 20.sp) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedAvatar) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76B947))
            ) {
                Text("Add", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

@Composable
fun PlayerListItem(player: Player, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF76B947)) else null,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF76B947).copy(alpha = 0.2f) else Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    player.avatar.ifEmpty { player.username.take(1).uppercase() },
                    fontSize = 22.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                player.username,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF76B947))
            }
        }
    }
}