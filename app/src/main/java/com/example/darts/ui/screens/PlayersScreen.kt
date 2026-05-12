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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.darts.db.entities.Battle
import com.example.darts.db.entities.Player
import com.example.darts.ui.viewModels.BattleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    modifier: Modifier = Modifier,
    battleViewModel: BattleViewModel = hiltViewModel(), // Use hiltViewModel
    onBattleCreated: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var showBattleNameDialog by remember { mutableStateOf(false) }
    var showDuplicateBattleDialog by remember { mutableStateOf(false) }
    var duplicateBattle by remember { mutableStateOf<Battle?>(null) }

    // Use 'by' to automatically unwrap the state
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
            title = { Text("Start Battle", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = battleName,
                    onValueChange = { battleViewModel.onNameChange(it) },
                    label = { Text("Battle Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        battleViewModel.saveBattle(
                            onSuccess = { newId ->
                                showBattleNameDialog = false
                                onBattleCreated() // Or pass newId if your navigation needs it
                            },
                            onDuplicateFound = { existing ->
                                showBattleNameDialog = false
                                duplicateBattle = existing // Trigger the "Duplicate" dialog
                            }
                        )
                    },
                    enabled = battleName.isNotBlank()
                ) {
                    Text("Ready")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBattleNameDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (duplicateBattle != null) {
        AlertDialog(
            onDismissRequest = { duplicateBattle = null },
            title = { Text("Battle Found") },
            text = {
                Text("A battle named '${duplicateBattle?.name}' already exists with these players. Would you like to use that one or create a new one anyway?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Option 1: Use Existing
                        val id = duplicateBattle?.idBattle
                        duplicateBattle = null
                        showDuplicateBattleDialog = false
                        onBattleCreated() // Navigate using existing battle logic
                    }
                ) { Text("Use Existing") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        battleViewModel.createNewBattle {  }
                    }
                ) { Text("Create New") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Players", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (selectedIds.size < 2) "Select 2-4" else "${selectedIds.size}/4 Selected",
                            fontSize = 12.sp,
                            color = if (selectedIds.size in 2..4) Color.Green else Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddPlayerDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Green)
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedIds.size in 2..4) {
                ExtendedFloatingActionButton(
                    onClick = { showBattleNameDialog = true },
                    containerColor = Color.Green,
                    contentColor = Color.Black,
                    text = { Text("Confirm Players") },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("Search...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // The filter happens on the 'players' State, so it updates automatically
                items(
                    items = players.filter { it.username.contains(searchQuery, ignoreCase = true) },
                    key = { it.idPlayer } // Key is essential for the list to update correctly
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
    }
}

@Composable
fun AddPlayerDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("") }
    val presets = listOf("🎯", "🔥", "🎲", "👤")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Player") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    presets.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .clip(CircleShape)
                                .background(if (selectedAvatar == emoji) Color.Green.copy(0.3f) else Color.DarkGray)
                                .clickable { selectedAvatar = emoji },
                            contentAlignment = Alignment.Center
                        ) { Text(emoji, fontSize = 20.sp) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, selectedAvatar) }) {
                Text("Add")
            }
        }
    )
}

@Composable
fun PlayerListItem(player: Player, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, Color.Green) else null,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(player.avatar.ifEmpty { player.username.take(1).uppercase() }, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(player.username, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}