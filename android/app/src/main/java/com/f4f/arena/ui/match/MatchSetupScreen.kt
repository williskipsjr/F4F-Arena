package com.f4f.arena.ui.match

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.f4f.arena.data.model.Match
import com.f4f.arena.data.model.MatchFormat
import com.f4f.arena.data.model.MatchStatus
import com.f4f.arena.data.model.MatchVoteDetail
import com.f4f.arena.ui.components.FootballLoadingAnimation
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(
    isAdmin: Boolean = true,
    isLoggedIn: Boolean = true,
    onRequireAuth: () -> Unit = {},
    viewModel: MatchViewModel = viewModel(),
    onOpenAttendance: (String) -> Unit,
    onOpenTeams: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var votesDialogMatch by remember { mutableStateOf<Match?>(null) }
    var votesLoading by remember { mutableStateOf(false) }
    var votesList by remember { mutableStateOf<List<MatchVoteDetail>>(emptyList()) }
    var votesError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun openVotesDialog(match: Match) {
        votesDialogMatch = match
        votesLoading = true
        votesError = null
        votesList = emptyList()
        viewModel.loadMatchVotes(match.id) { res ->
            votesLoading = false
            res.onSuccess { votesList = it }
                .onFailure { votesError = it.localizedMessage ?: "Failed to load votes" }
        }
    }

    val actionMessage = (uiState as? MatchUiState.Success)?.actionMessage
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
                title = { Text("Match Polls & Sessions", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadMatches() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Poll / Match") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is MatchUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FootballLoadingAnimation(message = "Loading matches...")
                    }
                }
                is MatchUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.loadMatches() }) { Text("Retry") }
                        }
                    }
                }
                is MatchUiState.Success -> {
                    if (state.matches.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("No match polls created yet", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (isAdmin) {
                                    Button(onClick = { showCreateDialog = true }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Create First Match Poll")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.matches, key = { it.id }) { match ->
                                MatchCard(
                                    match = match,
                                    isAdmin = isAdmin,
                                    onVote = { vote ->
                                        if (!isLoggedIn) {
                                            onRequireAuth()
                                        } else {
                                            viewModel.castVote(match.id, vote)
                                        }
                                    },
                                    onSelectFormat = { fmt ->
                                        viewModel.updateMatchFormat(match.id, fmt)
                                    },
                                    onViewVotes = { openVotesDialog(match) },
                                    onClick = {
                                        if (isAdmin) {
                                            onOpenAttendance(match.id)
                                        } else {
                                            openVotesDialog(match)
                                        }
                                    },
                                    onOpenTeams = { onOpenTeams(match.id) }
                                )
                            }
                        }
                    }
                }
            }

            if (showCreateDialog) {
                var isCreating by remember { mutableStateOf(false) }
                var createError by remember { mutableStateOf<String?>(null) }
                CreateMatchDialog(
                    isCreating = isCreating,
                    errorMessage = createError,
                    onDismiss = { if (!isCreating) showCreateDialog = false },
                    onConfirm = { title, kickoffTime, format, venue, isPollOpen ->
                        isCreating = true
                        createError = null
                        viewModel.createMatch(
                            title = title,
                            format = format,
                            venue = venue,
                            kickoffTime = kickoffTime,
                            isPollOpen = isPollOpen,
                            onSuccess = {
                                isCreating = false
                                showCreateDialog = false
                            },
                            onError = { msg ->
                                isCreating = false
                                createError = msg
                            }
                        )
                    }
                )
            }
            if (votesDialogMatch != null) {
                PollVotesDialog(
                    match = votesDialogMatch!!,
                    isLoading = votesLoading,
                    votes = votesList,
                    errorMessage = votesError,
                    onRefresh = { openVotesDialog(votesDialogMatch!!) },
                    onDismiss = { votesDialogMatch = null }
                )
            }
        }
    }
}

@Composable
fun MatchCard(
    match: Match,
    isAdmin: Boolean = true,
    onVote: (String) -> Unit = {},
    onSelectFormat: (MatchFormat) -> Unit = {},
    onViewVotes: () -> Unit = {},
    onClick: () -> Unit,
    onOpenTeams: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = match.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!match.kickoffTime.isNullOrBlank() || !match.venue.isNullOrBlank()) {
                        val details = listOfNotNull(
                            match.kickoffTime?.let { "🕙 $it" },
                            match.venue?.let { "📍 $it" }
                        ).joinToString("  •  ")
                        Text(
                            text = details,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                AssistChip(
                    onClick = {},
                    label = { Text(match.format.label, fontWeight = FontWeight.SemiBold) }
                )
            }

            // Poll Vote Tally & IN/OUT Buttons
            if (match.isPollOpen || match.inCount > 0 || match.outCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (match.isPollOpen) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = "POLL OPEN",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${match.inCount} IN  •  ${match.outCount} OUT",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = onViewVotes,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Who Voted")
                    }
                }

                if (match.isPollOpen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val votedIn = match.myVote == "IN"
                        val votedOut = match.myVote == "OUT"
                        Button(
                            onClick = { onVote("IN") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (votedIn) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (votedIn) "I'm IN ✓" else "Vote IN")
                        }
                        OutlinedButton(
                            onClick = { onVote("OUT") },
                            modifier = Modifier.weight(1f),
                            colors = if (votedOut) ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFFFEBEE),
                                contentColor = Color(0xFFC62828)
                            ) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(if (votedOut) "I'm OUT ✓" else "Vote OUT")
                        }
                    }
                }
            }

            // Admin Quick Format Selector when players have voted IN
            if (isAdmin && match.inCount > 0 && match.status != MatchStatus.FINALISED) {
                val suggested = match.inCount / match.format.playersPerTeam
                val subs = match.inCount % match.format.playersPerTeam
                Text(
                    text = "With ${match.inCount} IN (${match.format.label}): $suggested teams, $subs subs",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MatchFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = match.format == fmt,
                            onClick = { onSelectFormat(fmt) },
                            label = { Text(fmt.label, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: ${match.status.label}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (match.status == MatchStatus.TEAMS_GENERATED || match.status == MatchStatus.FINALISED || (isAdmin && match.inCount >= match.format.playersPerTeam)) {
                        OutlinedButton(
                            onClick = onOpenTeams,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(if (match.status == MatchStatus.TEAMS_GENERATED || match.status == MatchStatus.FINALISED) "View Teams" else "Generate Teams")
                        }
                    }
                    if (isAdmin) {
                        Button(
                            onClick = onClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Check In")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateMatchDialog(
    isCreating: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, MatchFormat, String?, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("Match tonight") }
    var kickoffTime by remember { mutableStateOf("10:00 PM") }
    var selectedFormat by remember { mutableStateOf(MatchFormat.SEVEN_A_SIDE) }
    var venue by remember { mutableStateOf("Turf Ground") }
    var openPoll by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Match Poll", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Match Title (e.g. Match tonight)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = kickoffTime,
                    onValueChange = { kickoffTime = it },
                    label = { Text("Kickoff Time (e.g. 10:00 PM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Format:", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MatchFormat.entries.forEach { fmt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFormat = fmt },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedFormat == fmt) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(fmt.label, fontWeight = FontWeight.Bold)
                                    Text(fmt.templateDescription, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (selectedFormat == fmt) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Open IN/OUT poll for players", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = openPoll, onCheckedChange = { openPoll = it })
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isCreating,
                onClick = {
                    val finalTitle = title.ifBlank { "Match tonight" }
                    onConfirm(
                        finalTitle,
                        kickoffTime.ifBlank { null },
                        selectedFormat,
                        venue.ifBlank { null },
                        openPoll
                    )
                }
            ) {
                Text(if (isCreating) "Creating..." else "Create Poll")
            }
        },
        dismissButton = {
            TextButton(enabled = !isCreating, onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PollVotesDialog(
    match: Match,
    isLoading: Boolean,
    votes: List<MatchVoteDetail>,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val inVotes = votes.filter { it.vote == "IN" }
    val outVotes = votes.filter { it.vote == "OUT" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("${match.title} — Votes", fontWeight = FontWeight.Bold)
                Text(
                    text = "${inVotes.size} IN • ${outVotes.size} OUT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 380.dp)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    errorMessage != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(errorMessage, color = MaterialTheme.colorScheme.error)
                            Button(onClick = onRefresh) { Text("Retry") }
                        }
                    }
                    votes.isEmpty() -> {
                        Text(
                            text = "No votes yet. Be the first to vote!",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (inVotes.isNotEmpty()) {
                                Text(
                                    text = "IN (${inVotes.size})",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                inVotes.forEach { v -> VoteRowItem(v, isIn = true) }
                            }
                            if (outVotes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "OUT (${outVotes.size})",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                outVotes.forEach { v -> VoteRowItem(v, isIn = false) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRefresh) { Text("Refresh") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun VoteRowItem(vote: MatchVoteDetail, isIn: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isIn) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${vote.primaryPosition.emoji} ${vote.displayName}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = vote.primaryPosition.label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatVoteTime(vote.votedAt),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatVoteTime(isoString: String): String {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"
    )
    for (pattern in formats) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(isoString) ?: continue
            val out = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            return out.format(date)
        } catch (_: Exception) {
        }
    }
    return isoString.take(16).replace("T", " ")
}
