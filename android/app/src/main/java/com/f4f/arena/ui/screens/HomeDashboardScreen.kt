package com.f4f.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f4f.arena.ui.components.GlassSearchBar
import com.f4f.arena.ui.components.PollAdminOverflowMenu
import com.f4f.arena.ui.components.PrimaryLimePillButton
import com.f4f.arena.ui.components.VoterAvatarStack
import com.f4f.arena.ui.state.*
import com.f4f.arena.ui.theme.F4FColors

@Composable
fun HomeDashboardScreen(
    session: UserSessionUi?,
    searchQuery: String,
    featuredPoll: MatchCardUi?,
    exploreMatches: List<MatchCardUi>,
    onSearchChange: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onVote: (matchId: String, voteIn: Boolean) -> Unit,
    onOpenVoters: (matchId: String) -> Unit,
    onSwitchFormat: (matchId: String, format: MatchFormat) -> Unit,
    onOpenTeams: (matchId: String) -> Unit,
    onOpenCheckIn: (matchId: String) -> Unit,
    onEndMatch: (matchId: String) -> Unit = {},
    onDeleteMatch: (matchId: String) -> Unit = {},
    onSeeAllMatches: () -> Unit,
    onCreateNewPoll: () -> Unit
) {
    val isAdmin = session?.role == UserRole.ADMIN

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(F4FColors.CanvasOffWhite)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
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
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "Hello! ",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                                Text(
                                    text = session?.name ?: "Guest",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "F4F Community · ${if (isAdmin) "Admin" else if (session != null) "Player" else "Tap avatar to Sign In"}",
                                    color = Color.White.copy(alpha = 0.84f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                                .clickable(onClick = onAvatarClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (session?.name?.take(2) ?: "IN").uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    GlassSearchBar(
                        query = searchQuery,
                        onQueryChange = onSearchChange,
                        placeholder = "Search matches or venues..."
                    )
                }
            }

            if (featuredPoll != null) {
                item {
                    FeaturedPollCard(
                        poll = featuredPoll,
                        isAdmin = isAdmin,
                        onVote = { voteIn -> onVote(featuredPoll.id, voteIn) },
                        onOpenVoters = { onOpenVoters(featuredPoll.id) },
                        onSwitchFormat = { fmt -> onSwitchFormat(featuredPoll.id, fmt) },
                        onOpenTeams = { onOpenTeams(featuredPoll.id) },
                        onEndMatch = { onEndMatch(featuredPoll.id) },
                        onDeleteMatch = { onDeleteMatch(featuredPoll.id) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Explore Matches",
                        style = MaterialTheme.typography.titleLarge,
                        color = F4FColors.TextPrimary
                    )
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelMedium,
                        color = F4FColors.ForestMid,
                        modifier = Modifier.clickable(onClick = onSeeAllMatches)
                    )
                }
            }

            items(exploreMatches, key = { it.id }) { match ->
                ExploreMatchCard(
                    match = match,
                    isAdmin = isAdmin,
                    onViewTeams = { onOpenTeams(match.id) },
                    onCheckIn = { onOpenCheckIn(match.id) },
                    onEndMatch = { onEndMatch(match.id) },
                    onDeleteMatch = { onDeleteMatch(match.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }

        if (isAdmin) {
            ExtendedFloatingActionButton(
                onClick = onCreateNewPoll,
                containerColor = F4FColors.ForestDeep,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 108.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("New Poll", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FeaturedPollCard(
    poll: MatchCardUi,
    isAdmin: Boolean,
    onVote: (Boolean) -> Unit,
    onOpenVoters: () -> Unit,
    onSwitchFormat: (MatchFormat) -> Unit,
    onOpenTeams: () -> Unit,
    onEndMatch: () -> Unit,
    onDeleteMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = F4FColors.ForestDeep.copy(alpha = 0.14f))
            .then(
                if (poll.isEnded) Modifier.border(1.5.dp, Color(0xFFFCA5A5), RoundedCornerShape(24.dp))
                else Modifier
            )
            .clickable(onClick = onOpenVoters),
        shape = RoundedCornerShape(24.dp),
        color = if (poll.isEnded) Color(0xFFFEF2F2) else F4FColors.CardWhite
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (poll.isEnded) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFDC2626))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("MATCH ENDED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Text(
                            text = if (poll.pollOpen) "Poll open · ${poll.dateLabel}" else "Poll closed · ${poll.dateLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = F4FColors.ForestMid
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Votes & Comments",
                        style = MaterialTheme.typography.labelMedium,
                        color = F4FColors.ForestMid,
                        modifier = Modifier.clickable(onClick = onOpenVoters)
                    )
                    if (isAdmin) {
                        PollAdminOverflowMenu(
                            matchTitle = poll.title,
                            isEnded = poll.isEnded,
                            onEndMatch = onEndMatch,
                            onDeleteMatch = onDeleteMatch
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = poll.title,
                style = MaterialTheme.typography.headlineMedium,
                color = F4FColors.TextPrimary
            )

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onOpenVoters)
            ) {
                VoterAvatarStack(initials = poll.voterInitials)
                if (poll.voterInitials.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = "${poll.inCount} IN · ${poll.outCount} OUT",
                    style = MaterialTheme.typography.titleMedium,
                    color = F4FColors.TextPrimary
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "${poll.timelineLabel} · ${poll.venue} · ${poll.format.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (poll.isEnded) Color(0xFFB91C1C) else if (poll.isOngoing) F4FColors.PitchGreen else F4FColors.TextSecondary,
                fontWeight = if (poll.isOngoing || poll.isEnded) FontWeight.SemiBold else FontWeight.Normal
            )

            if (poll.isEnded && poll.feePerPlayer != null && poll.turfFee != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEE2E2))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Payment Due: ₹${poll.feePerPlayer} / player (Total Turf Fee ₹${poll.turfFee})",
                        color = Color(0xFF991B1B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val votedIn = poll.currentUserVoteIn == true
                val votedOut = poll.currentUserVoteIn == false

                PrimaryLimePillButton(
                    text = if (votedIn) "Voted IN" else "Vote IN",
                    icon = Icons.Outlined.ThumbUp,
                    onClick = { onVote(true) },
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = { onVote(false) },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (votedOut) F4FColors.NavCharcoal else F4FColors.SurfaceMuted,
                        contentColor = if (votedOut) Color.White else F4FColors.TextPrimary
                    ),
                    border = null
                ) {
                    Text(
                        text = if (votedOut) "Voted OUT" else "Vote OUT",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = F4FColors.HairlineBorder)
            Spacer(Modifier.height(12.dp))

            if (isAdmin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MatchFormat.entries.forEach { fmt ->
                        val active = poll.format == fmt
                        Box(
                            modifier = Modifier
                                .widthIn(min = 84.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) F4FColors.ForestDeep else F4FColors.SurfaceMuted)
                                .clickable { onSwitchFormat(fmt) }
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${fmt.size}v${fmt.size}",
                                    color = if (active) F4FColors.PitchLime else F4FColors.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = fmt.mathSummary(poll.inCount),
                                    color = if (active) Color.White.copy(alpha = 0.85f) else F4FColors.TextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                if (poll.inCount >= poll.format.size) {
                    Spacer(Modifier.height(10.dp))
                    PrimaryLimePillButton(
                        text = if (poll.teamsReady) "Open Team Board" else "Generate Teams (${poll.format.label})",
                        icon = Icons.Outlined.SportsSoccer,
                        onClick = onOpenTeams,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${poll.format.label} · ${poll.format.mathSummary(poll.inCount)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = F4FColors.TextSecondary
                    )
                    Text(
                        text = "View Teams",
                        style = MaterialTheme.typography.labelMedium,
                        color = F4FColors.ForestMid,
                        modifier = Modifier.clickable(onClick = onOpenTeams)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreMatchCard(
    match: MatchCardUi,
    isAdmin: Boolean,
    onViewTeams: () -> Unit,
    onCheckIn: () -> Unit,
    onEndMatch: () -> Unit,
    onDeleteMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(22.dp), ambientColor = F4FColors.ForestDeep.copy(alpha = 0.08f))
            .then(
                if (match.isEnded) Modifier.border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(22.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(22.dp),
        color = if (match.isEnded) Color(0xFFFEF2F2) else F4FColors.CardWhite
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = F4FColors.TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (match.isOngoing) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("ONGOING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = F4FColors.ForestDeep)
                        }
                    } else if (match.isEnded) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(F4FColors.SurfaceMuted)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("ENDED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = F4FColors.TextMuted)
                        }
                    }
                    Text(
                        text = match.format.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = F4FColors.ForestMid
                    )
                    if (isAdmin) {
                        PollAdminOverflowMenu(
                            matchTitle = match.title,
                            isEnded = match.isEnded,
                            onEndMatch = onEndMatch,
                            onDeleteMatch = onDeleteMatch
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${match.timelineLabel} · ${match.inCount} IN · ${if (match.isEnded) "Completed" else if (match.finalised) "Finalised" else if (match.teamsReady) "Teams Ready" else "Draft"}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (match.isOngoing) F4FColors.ForestMid else F4FColors.TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onViewTeams,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Teams", fontWeight = FontWeight.SemiBold, color = F4FColors.TextPrimary)
                }
                if (isAdmin && !match.isEnded) {
                    Button(
                        onClick = onCheckIn,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = F4FColors.ForestDeep,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Check In", fontWeight = FontWeight.SemiBold)
                    }
                    if (match.teamsReady || match.isOngoing) {
                        OutlinedButton(
                            onClick = onEndMatch,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("End", fontWeight = FontWeight.SemiBold, color = F4FColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
