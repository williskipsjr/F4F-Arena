package com.f4f.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.f4f.arena.ui.components.PrimaryLimePillButton
import com.f4f.arena.ui.state.*
import com.f4f.arena.ui.theme.F4FColors

@Composable
fun RosterScreen(
    players: List<PlayerUi>,
    isAdmin: Boolean,
    onEditPlayer: (PlayerUi) -> Unit,
    onToggleActive: (playerId: String, active: Boolean) -> Unit,
    onAddPlayer: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedPos by remember { mutableStateOf<PlayerPosition?>(null) }

    val filtered = players.filter { p ->
        (selectedPos == null || p.position == selectedPos) &&
            p.name.contains(search, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(F4FColors.CanvasOffWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            Text("COMMUNITY ROSTER", style = MaterialTheme.typography.displaySmall)
            Text(
                "${players.count { it.isActive }} active · ${players.size} total members",
                style = MaterialTheme.typography.labelMedium,
                color = F4FColors.TextSecondary
            )

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search roster...") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs: List<PlayerPosition?> = listOf(null) + PlayerPosition.entries
                tabs.forEach { pos ->
                    val count = if (pos == null) players.size else players.count { it.position == pos }
                    FilterChip(
                        selected = selectedPos == pos,
                        onClick = { selectedPos = pos },
                        label = { Text("${pos?.shortCode ?: "All"} ($count)") }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 112.dp)
            ) {
                items(filtered, key = { it.id }) { player ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isAdmin) { onEditPlayer(player) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(player.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${player.position.displayName} · ${if (player.isActive) "Active" else "Inactive"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = F4FColors.TextSecondary
                                )
                            }
                            if (isAdmin) {
                                Switch(
                                    checked = player.isActive,
                                    onCheckedChange = { onToggleActive(player.id, it) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            FloatingActionButton(
                onClick = onAddPlayer,
                containerColor = F4FColors.ForestDeep,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 96.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Player")
            }
        }
    }
}

@Composable
fun SystemHealthScreen(
    health: SystemHealthUi,
    session: UserSessionUi?,
    onRecheck: () -> Unit,
    onOpenAuth: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(F4FColors.CanvasOffWhite)
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("SYSTEM & SESSION", style = MaterialTheme.typography.displaySmall)

        Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Backend Connection", style = MaterialTheme.typography.titleMedium)
                Text(health.baseUrl, style = MaterialTheme.typography.bodyMedium, color = F4FColors.TextSecondary)
                HorizontalDivider(color = F4FColors.HairlineBorder)
                Text("API Gateway: ${if (health.apiConnected) "Connected" else "Disconnected"}")
                Text("PostgreSQL: ${if (health.postgresConnected) "Connected" else "Disconnected"}")
                Text("Last checked: ${health.lastChecked}", style = MaterialTheme.typography.labelMedium, color = F4FColors.TextMuted)
                Spacer(Modifier.height(6.dp))
                PrimaryLimePillButton(
                    text = "Recheck Status",
                    icon = Icons.Outlined.Refresh,
                    onClick = onRecheck,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current Session", style = MaterialTheme.typography.titleMedium)
                if (session != null) {
                    Text("${session.name} (@${session.username}) · ${session.role.name}")
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out")
                    }
                } else {
                    Text("Not signed in", color = F4FColors.TextSecondary)
                    PrimaryLimePillButton(text = "Sign In / Sign Up", onClick = onOpenAuth, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
