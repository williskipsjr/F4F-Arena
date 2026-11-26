package com.f4f.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun UpdatesAndFeedbackScreen(
    health: SystemHealthUi,
    session: UserSessionUi?,
    feedbackItems: List<FeedbackItemUi> = emptyList(),
    isSubmittingFeedback: Boolean = false,
    onSubmitFeedback: (category: String, message: String) -> Unit,
    onRecheck: () -> Unit,
    onOpenAuth: () -> Unit,
    onSignOut: () -> Unit
) {
    val isAdmin = session?.role == UserRole.ADMIN
    var selectedCategory by remember { mutableStateOf("FEATURE") }
    var feedbackText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(F4FColors.CanvasOffWhite)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("UPDATES & FEEDBACK", style = MaterialTheme.typography.displaySmall)

        // Upcoming Roadmap Card (Everyone)
        Surface(shape = RoundedCornerShape(22.dp), color = F4FColors.ForestDeep, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = F4FColors.PitchLime)
                    Spacer(Modifier.width(8.dp))
                    Text("Upcoming Updates", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                RoadmapItem(
                    badge = "NEXT UPDATE",
                    title = "Post-Match: UPI Payment Router",
                    subtitle = "Automatic turf fee split & 1-tap UPI payment links after each match."
                )
                RoadmapItem(
                    badge = "COMING SOON",
                    title = "Player Attendance & Reliability Stats",
                    subtitle = "Track matches played, streaks, and positional history."
                )
                RoadmapItem(
                    badge = "PLANNED",
                    title = "Match Scoreboard & MVP Voting",
                    subtitle = "Log final scores and vote Man of the Match."
                )
            }
        }

        // Feedback Submission Card (Everyone)
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Help Improve F4F Arena", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Share feature ideas, bugs, or match-day suggestions directly with the organiser.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = F4FColors.TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("FEATURE" to "Feature Idea", "BUG" to "Bug Report", "UI" to "UI / UX").forEach { (cat, label) ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    placeholder = { Text("Write your feedback or suggestion...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryLimePillButton(
                    text = if (isSubmittingFeedback) "Sending..." else if (session == null) "Sign In to Send Feedback" else "Submit Feedback",
                    icon = Icons.Outlined.Send,
                    enabled = !isSubmittingFeedback && (session == null || feedbackText.isNotBlank()),
                    onClick = {
                        if (session == null) {
                            onOpenAuth()
                        } else {
                            onSubmitFeedback(selectedCategory, feedbackText.trim())
                            feedbackText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Current Session Card (Everyone)
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

        // Admin-Only System Diagnostics & Feedback Inbox
        if (isAdmin) {
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Admin · System Diagnostics", style = MaterialTheme.typography.titleMedium)
                    Text(health.baseUrl, style = MaterialTheme.typography.bodyMedium, color = F4FColors.TextSecondary)
                    HorizontalDivider(color = F4FColors.HairlineBorder)
                    Text("API Gateway: ${if (health.apiConnected) "Connected (${health.latencyMs} ms)" else "Disconnected"}")
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

            if (feedbackItems.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Admin · Community Feedback (${feedbackItems.size})", style = MaterialTheme.typography.titleMedium)
                        feedbackItems.forEach { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(F4FColors.SurfaceMuted)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "${item.authorName} · ${item.category} · ${item.createdAtLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = F4FColors.ForestMid
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(item.message, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoadmapItem(badge: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = F4FColors.PitchLime)
        Spacer(Modifier.height(2.dp))
        Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        Text(subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}
