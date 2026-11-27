package com.f4f.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f4f.arena.ui.components.FootballLoadingAnimation
import com.f4f.arena.ui.state.PollCommentUi
import com.f4f.arena.ui.state.PollVoteUi
import com.f4f.arena.ui.theme.F4FColors

private enum class PollSheetTab { COMMENTS, VOTES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollVotersSheet(
    matchTitle: String,
    votes: List<PollVoteUi>,
    comments: List<PollCommentUi> = emptyList(),
    isLoading: Boolean = false,
    isLoggedIn: Boolean = false,
    isAdmin: Boolean = false,
    isPostingComment: Boolean = false,
    onRefresh: () -> Unit,
    onPostComment: (String) -> Unit = {},
    onDeleteComment: (String) -> Unit = {},
    onRequireSignIn: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val inVotes = votes.filter { it.isIn }
    val outVotes = votes.filter { !it.isIn }
    var activeTab by remember { mutableStateOf(PollSheetTab.VOTES) }
    var commentDraft by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = F4FColors.CanvasOffWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(matchTitle, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "YES (IN): ${inVotes.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = F4FColors.ForestDeep
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(F4FColors.SurfaceMuted)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "NO (OUT): ${outVotes.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = F4FColors.TextSecondary
                            )
                        }
                    }
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(F4FColors.SurfaceMuted)
                    .padding(4.dp)
            ) {
                listOf(
                    PollSheetTab.VOTES to "Votes (${votes.size})",
                    PollSheetTab.COMMENTS to "Comments (${comments.size})"
                ).forEach { (tab, label) ->
                    val selected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) F4FColors.ForestDeep else Color.Transparent)
                            .clickable { activeTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else F4FColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FootballLoadingAnimation(message = "Loading poll details...")
                }
            } else if (activeTab == PollSheetTab.VOTES) {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Text(
                            "YES · IN (${inVotes.size})",
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
                                "NO · OUT (${outVotes.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = F4FColors.TextSecondary
                            )
                        }
                        items(outVotes, key = { "out_${it.playerId}" }) { vote ->
                            VoterRowCard(vote)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    if (comments.isEmpty()) {
                        item {
                            Text(
                                "No comments yet. Leave a note (e.g. reason if OUT, or kit instructions).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = F4FColors.TextSecondary,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(comments, key = { it.id }) { comment ->
                            CommentCard(
                                comment = comment,
                                canDelete = isAdmin,
                                onDelete = { onDeleteComment(comment.id) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { commentDraft = it },
                        placeholder = {
                            Text(
                                if (isLoggedIn) "Add a note (e.g. Wear Red/Blue or reason)..."
                                else "Sign in to post a comment..."
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    )
                    FilledIconButton(
                        enabled = !isPostingComment && (!isLoggedIn || commentDraft.isNotBlank()),
                        onClick = {
                            if (!isLoggedIn) {
                                onRequireSignIn()
                            } else {
                                val msg = commentDraft.trim()
                                if (msg.isNotEmpty()) {
                                    onPostComment(msg)
                                    commentDraft = ""
                                }
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = F4FColors.ForestDeep,
                            contentColor = F4FColors.PitchLime
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send")
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
private fun CommentCard(
    comment: PollCommentUi,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        comment.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = F4FColors.TextPrimary
                    )
                    if (comment.isAdmin) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(F4FColors.ForestDeep)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ADMIN",
                                color = F4FColors.PitchLime,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Text(
                        comment.createdAtLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = F4FColors.TextMuted
                    )
                }
                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete comment",
                            tint = F4FColors.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = comment.message,
                style = MaterialTheme.typography.bodyMedium,
                color = F4FColors.TextPrimary
            )
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
