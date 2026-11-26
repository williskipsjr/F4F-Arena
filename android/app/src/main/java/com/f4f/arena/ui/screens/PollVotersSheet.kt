package com.f4f.arena.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.f4f.arena.ui.components.FootballLoadingAnimation
import com.f4f.arena.ui.state.PollVoteUi
import com.f4f.arena.ui.theme.F4FColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollVotersSheet(
    matchTitle: String,
    votes: List<PollVoteUi>,
    isLoading: Boolean = false,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val inVotes = votes.filter { it.isIn }
    val outVotes = votes.filter { !it.isIn }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = F4FColors.CanvasOffWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("$matchTitle — Votes", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${inVotes.size} IN · ${outVotes.size} OUT",
                        style = MaterialTheme.typography.labelMedium,
                        color = F4FColors.ForestMid
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh votes")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FootballLoadingAnimation(message = "Loading voters...")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Text(
                            "IN (${inVotes.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = F4FColors.ForestMid
                        )
                    }
                    items(inVotes, key = { "in_${it.playerId}" }) { vote ->
                        VoterRowCard(vote)
                    }

                    if (outVotes.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "OUT (${outVotes.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = F4FColors.TextSecondary
                            )
                        }
                        items(outVotes, key = { "out_${it.playerId}" }) { vote ->
                            VoterRowCard(vote)
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = F4FColors.NavCharcoal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(48.dp)
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun VoterRowCard(vote: PollVoteUi) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(vote.playerName, style = MaterialTheme.typography.titleMedium)
                Text(
                    vote.position.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = F4FColors.TextSecondary
                )
            }
            Text(
                vote.votedAtLabel,
                style = MaterialTheme.typography.labelMedium,
                color = F4FColors.TextMuted
            )
        }
    }
}
