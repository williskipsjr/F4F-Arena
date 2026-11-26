package com.f4f.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.f4f.arena.ui.components.PrimaryLimePillButton
import com.f4f.arena.ui.state.*
import com.f4f.arena.ui.theme.F4FColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceCheckInScreen(
    format: MatchFormat,
    players: List<PlayerUi>,
    checkedInIds: Set<String>,
    isSaving: Boolean = false,
    onBack: () -> Unit,
    onTogglePlayer: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onQuickAddGuest: () -> Unit,
    onSaveAttendance: () -> Unit,
    onGenerateTeams: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var filterPos by remember { mutableStateOf<PlayerPosition?>(null) }

    val checkedPlayers = players.filter { it.id in checkedInIds }
    val teamsCount = checkedPlayers.size / format.size
    val subsCount = checkedPlayers.size % format.size
    val gkCount = checkedPlayers.count { it.position == PlayerPosition.GK }
    val defCount = checkedPlayers.count { it.position == PlayerPosition.DEF }
    val midCount = checkedPlayers.count { it.position == PlayerPosition.MID }
    val strCount = checkedPlayers.count { it.position == PlayerPosition.STR }
    val keeperShortage = (teamsCount - gkCount).coerceAtLeast(0)

    val filtered = players.filter { p ->
        (filterPos == null || p.position == filterPos) &&
            p.name.contains(search, ignoreCase = true)
    }

    Scaffold(
        containerColor = F4FColors.CanvasOffWhite,
        topBar = {
            TopAppBar(
                title = { Text("Match Attendance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onQuickAddGuest) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = "Add Guest")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = F4FColors.CanvasOffWhite)
            )
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 10.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveAttendance,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(if (isSaving) "Saving..." else "Save", fontWeight = FontWeight.SemiBold)
                    }
                    PrimaryLimePillButton(
                        text = "Generate Teams",
                        enabled = checkedPlayers.size >= format.size && !isSaving,
                        onClick = onGenerateTeams,
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = F4FColors.ForestDeep,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${checkedPlayers.size} Checked In",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            format.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = F4FColors.PitchLime
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Yields: $teamsCount Teams · $subsCount Substitute${if (subsCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "$gkCount GK · $defCount DEF · $midCount MID · $strCount STR",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    if (keeperShortage > 0) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(F4FColors.WarningBg)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = F4FColors.WarningText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Keeper shortage: Need $keeperShortage GK!",
                                color = F4FColors.WarningText,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search player") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onSelectAll) { Text("All") }
                TextButton(onClick = onClearAll) { Text("Clear") }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val options: List<PlayerPosition?> = listOf(null) + PlayerPosition.entries
                options.forEach { pos ->
                    val active = filterPos == pos
                    FilterChip(
                        selected = active,
                        onClick = { filterPos = pos },
                        label = { Text(pos?.shortCode ?: "All") }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filtered, key = { it.id }) { player ->
                    val checked = player.id in checkedInIds
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTogglePlayer(player.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = { onTogglePlayer(player.id) })
                            Spacer(Modifier.width(8.dp))
                            Text(
                                player.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                player.position.shortCode,
                                style = MaterialTheme.typography.labelMedium,
                                color = F4FColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
