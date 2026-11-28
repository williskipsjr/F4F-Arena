package com.f4f.arena.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f4f.arena.ui.components.PollAdminOverflowMenu
import com.f4f.arena.ui.components.PrimaryLimePillButton
import com.f4f.arena.ui.components.VoterAvatarStack
import com.f4f.arena.ui.state.MatchCardUi
import com.f4f.arena.ui.theme.F4FColors

@Composable
fun AllMatchesScreen(
    matches: List<MatchCardUi>,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onOpenVoters: (String) -> Unit,
    onOpenTeams: (String) -> Unit,
    onOpenCheckIn: (String) -> Unit,
    onDeleteMatch: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(F4FColors.CanvasOffWhite)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(F4FColors.HeroVerticalBrush)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 112.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassCircleIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
                        Text(
                            text = "Explore Matches",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassCircleIcon(Icons.Outlined.Search, "Search") {}
                            GlassCircleIcon(Icons.Outlined.Tune, "Filter") {}
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Text(
                        text = "ALL MATCHES",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Vote on active polls, review attendance math, and jump straight into balanced squads.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
            }

            items(matches, key = { it.id }) { match ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = F4FColors.ForestDeep.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${match.format.label} · ${match.timelineLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (match.isOngoing) F4FColors.PitchGreen else F4FColors.TextSecondary
                            )
                            if (isAdmin) {
                                PollAdminOverflowMenu(
                                    matchTitle = match.title,
                                    isEnded = match.isEnded,
                                    onDeleteMatch = { onDeleteMatch(match.id) }
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = match.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = F4FColors.TextPrimary
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenVoters(match.id) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                VoterAvatarStack(match.voterInitials)
                                if (match.voterInitials.isNotEmpty()) {
                                    Spacer(Modifier.width(10.dp))
                                }
                                Text(
                                    text = "${match.inCount} IN · ${match.format.mathSummary(match.inCount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (!match.postMatchPhotoUrl.isNullOrBlank()) {
                            Spacer(Modifier.height(14.dp))
                            MatchPostPhoto(photoUrl = match.postMatchPhotoUrl)
                            Spacer(Modifier.height(10.dp))
                        }
                        if (match.feePerPlayer != null && match.turfFee != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFDCFCE7))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Turf Fee: ₹${match.feePerPlayer} / player (Total ₹${match.turfFee})",
                                    color = F4FColors.ForestDeep,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrimaryLimePillButton(
                                text = "View Teams",
                                onClick = { onOpenTeams(match.id) },
                                modifier = Modifier.weight(1f)
                            )
                            if (isAdmin) {
                                OutlinedButton(
                                    onClick = { onOpenCheckIn(match.id) },
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Check In", fontWeight = FontWeight.SemiBold, color = F4FColors.TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassCircleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}
