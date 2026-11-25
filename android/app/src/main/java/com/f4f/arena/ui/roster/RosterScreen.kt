package com.f4f.arena.ui.roster

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.f4f.arena.data.model.Player
import com.f4f.arena.data.model.Position

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    viewModel: RosterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val actionMessage = (uiState as? RosterUiState.Success)?.actionMessage
    LaunchedEffect(actionMessage) {
        if (actionMessage != null) {
            snackbarHostState.showSnackbar(actionMessage)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Player Roster", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadRoster() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Roster",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Player")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is RosterUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is RosterUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.loadRoster() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is RosterUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Box
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text("Search 42 community players...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Positional Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.selectedFilter == null,
                                onClick = { viewModel.onFilterSelected(null) },
                                label = { Text("All (${state.totalCount})") }
                            )
                            FilterChip(
                                selected = state.selectedFilter == Position.KEEPER,
                                onClick = { viewModel.onFilterSelected(Position.KEEPER) },
                                label = { Text("🧤 GK (${state.keeperCount})") }
                            )
                            FilterChip(
                                selected = state.selectedFilter == Position.DEFENDER,
                                onClick = { viewModel.onFilterSelected(Position.DEFENDER) },
                                label = { Text("🛡️ DEF (${state.defenderCount})") }
                            )
                            FilterChip(
                                selected = state.selectedFilter == Position.MIDFIELDER,
                                onClick = { viewModel.onFilterSelected(Position.MIDFIELDER) },
                                label = { Text("⚙️ MID (${state.midfielderCount})") }
                            )
                            FilterChip(
                                selected = state.selectedFilter == Position.STRIKER,
                                onClick = { viewModel.onFilterSelected(Position.STRIKER) },
                                label = { Text("⚡ STR (${state.strikerCount})") }
                            )
                        }

                        Divider(modifier = Modifier.padding(top = 8.dp))

                        // Players List
                        if (state.filteredPlayers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No players match the filter",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.filteredPlayers, key = { it.id }) { player ->
                                    PlayerCard(
                                        player = player,
                                        onToggleActive = { viewModel.togglePlayerActive(player) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showAddDialog) {
                AddPlayerDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name, position, notes ->
                        viewModel.createPlayer(name, position, notes)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun PlayerCard(
    player: Player,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (player.isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(positionColor(player.primaryPosition).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.primaryPosition.emoji,
                        fontSize = 20.sp
                    )
                }

                Column {
                    Text(
                        text = player.displayName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (player.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = player.primaryPosition.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = positionColor(player.primaryPosition)
                        )
                        if (!player.isActive) {
                            Text(
                                text = "• Inactive",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Switch(
                checked = player.isActive,
                onCheckedChange = { onToggleActive() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Position, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedPosition by remember { mutableStateOf(Position.MIDFIELDER) }
    var notes by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Player to Roster", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("Player Display Name") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Primary Position:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Position.entries.forEach { pos ->
                        FilterChip(
                            selected = selectedPosition == pos,
                            onClick = { selectedPosition = pos },
                            label = { Text("${pos.emoji} ${pos.shortLabel}") }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onConfirm(name.trim(), selectedPosition, notes.ifBlank { null })
                    }
                }
            ) {
                Text("Add Player")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun positionColor(pos: Position): Color {
    return when (pos) {
        Position.KEEPER -> Color(0xFFE65100)     // Amber/Orange
        Position.DEFENDER -> Color(0xFF1565C0)   // Deep Blue
        Position.MIDFIELDER -> Color(0xFF2E7D32) // Pitch Green
        Position.STRIKER -> Color(0xFFC62828)    // Crimson Red
    }
}
