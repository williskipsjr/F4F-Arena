package com.f4f.arena.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import com.f4f.arena.ui.roster.AddPlayerDialog
import com.f4f.arena.ui.roster.positionColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    matchId: String,
    onBack: () -> Unit,
    onProceedToMatchmaking: (String) -> Unit,
    viewModel: AttendanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showGuestDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(matchId) {
        viewModel.loadMatchAttendance(matchId)
    }

    val saveMessage = (uiState as? AttendanceUiState.Success)?.saveMessage
    LaunchedEffect(saveMessage) {
        if (saveMessage != null) {
            snackbarHostState.showSnackbar(saveMessage)
            viewModel.clearSaveMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Match Attendance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { showGuestDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Quick Add Guest", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        bottomBar = {
            if (uiState is AttendanceUiState.Success) {
                val state = uiState as AttendanceUiState.Success
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.saveAttendance() },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isSaving
                            ) {
                                Text(if (state.isSaving) "Saving..." else "Save Attendance")
                            }

                            Button(
                                onClick = {
                                    viewModel.saveAttendance {
                                        onProceedToMatchmaking(matchId)
                                    }
                                },
                                modifier = Modifier.weight(1.2f),
                                enabled = state.totalSelected >= state.match.format.playersPerTeam
                            ) {
                                Text("Generate Teams ⚽")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is AttendanceUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AttendanceUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.loadMatchAttendance(matchId) }) { Text("Retry") }
                        }
                    }
                }
                is AttendanceUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Live Sticky Summary Header Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${state.totalSelected} Attendees Checked In",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = state.match.format.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = "Yields: ${state.suggestedTeams} full teams • ${state.substitutesCount} substitutes",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                // Positional breakdown chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PositionBadgeMini("🧤 ${state.keepersSelected} GK", Position.KEEPER)
                                    PositionBadgeMini("🛡️ ${state.defendersSelected} DEF", Position.DEFENDER)
                                    PositionBadgeMini("⚙️ ${state.midfieldersSelected} MID", Position.MIDFIELDER)
                                    PositionBadgeMini("⚡ ${state.strikersSelected} STR", Position.STRIKER)
                                }

                                if (state.keeperDeficit > 0) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFFF3E0))
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Keeper shortage: Need ${state.keeperDeficit} emergency GK!",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                            }
                        }

                        // Search & Filters
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text("Search player name...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { viewModel.selectAllActive() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("All")
                            }
                            OutlinedButton(
                                onClick = { viewModel.clearSelection() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Clear")
                            }
                        }

                        // Positional Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = state.selectedFilter == null,
                                onClick = { viewModel.onFilterSelected(null) },
                                label = { Text("All (${state.allPlayers.size})") }
                            )
                            FilterChip(
                                selected = state.selectedFilter == Position.KEEPER,
                                onClick = { viewModel.onFilterSelected(Position.KEEPER) },
                                label = { Text("🧤 GK") }
                            )
                            FilterChip(
                                selected = state.selectedFilter == Position.DEFENDER,
                                onClick = { viewModel.onFilterSelected(Position.DEFENDER) },
                                label = { Text("🛡️ DEF") }
                            )
                            FilterChip(
                                selected = state.selectedFilter == Position.MIDFIELDER,
                                onClick = { viewModel.onFilterSelected(Position.MIDFIELDER) },
                                label = { Text("⚙️ MID") }
                            )
                            FilterChip(
                                selected = state.selectedFilter == Position.STRIKER,
                                onClick = { viewModel.onFilterSelected(Position.STRIKER) },
                                label = { Text("⚡ STR") }
                            )
                        }

                        Divider()

                        // Attendance Checklist
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.filteredPlayers, key = { it.id }) { player ->
                                val isChecked = state.selectedPlayerIds.contains(player.id)
                                AttendanceRow(
                                    player = player,
                                    isChecked = isChecked,
                                    onToggle = { viewModel.togglePlayer(player.id) }
                                )
                            }
                        }
                    }
                }
            }

            if (showGuestDialog) {
                AddPlayerDialog(
                    onDismiss = { showGuestDialog = false },
                    onConfirm = { name, pos, _ ->
                        viewModel.quickAddGuest(name, pos)
                        showGuestDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun AttendanceRow(
    player: Player,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onToggle() }
                )
                Text(
                    text = player.displayName,
                    fontSize = 16.sp,
                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                )
            }

            AssistChip(
                onClick = onToggle,
                label = { Text("${player.primaryPosition.emoji} ${player.primaryPosition.shortLabel}") }
            )
        }
    }
}

@Composable
fun PositionBadgeMini(text: String, pos: Position) {
    Surface(
        color = positionColor(pos).copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = positionColor(pos),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
