package com.f4f.arena.ui.match

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.f4f.arena.data.model.MatchStatus
import com.f4f.arena.data.model.Position
import com.f4f.arena.data.model.SubstitutePolicy
import com.f4f.arena.data.model.TeamDetail
import com.f4f.arena.data.model.TeamPlayerDetail
import com.f4f.arena.ui.roster.positionColor
import com.f4f.arena.util.WhatsAppFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamBoardScreen(
    matchId: String,
    onBack: () -> Unit,
    viewModel: TeamBoardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFinaliseDialog by remember { mutableStateOf(false) }
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var copiedToClipboard by remember { mutableStateOf(false) }
    var movingPlayer by remember { mutableStateOf<TeamPlayerDetail?>(null) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(matchId) {
        viewModel.loadOrGenerateTeams(matchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Teams", fontWeight = FontWeight.Bold) },
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
                    val successState = uiState as? TeamBoardUiState.Success
                    if (successState != null) {
                        IconButton(onClick = {
                            copiedToClipboard = false
                            showWhatsAppDialog = true
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Export to WhatsApp", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        if (successState.data.status != MatchStatus.FINALISED) {
                            IconButton(onClick = { viewModel.generateTeams(matchId) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                            IconButton(onClick = { showFinaliseDialog = true }) {
                                Icon(Icons.Default.Lock, contentDescription = "Finalise Teams", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is TeamBoardUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator()
                            Text("Loading teams...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is TeamBoardUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                            Button(onClick = { viewModel.loadOrGenerateTeams(matchId) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is TeamBoardUiState.Success -> {
                    val data = state.data
                    val isFinalised = data.status == MatchStatus.FINALISED

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Swap Mode Active Banner
                        if (state.selectedPlayerForSwap != null) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Swap Mode Active",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Text(
                                                    text = "Tap any other player to swap with ${state.selectedPlayerForSwap.displayName}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        TextButton(onClick = { viewModel.clearSwapSelection() }) {
                                            Text("Cancel")
                                        }
                                    }
                                }
                            }
                        }

                        // Banner feedback message
                        if (state.bannerMessage != null && state.selectedPlayerForSwap == null) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = state.bannerMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Overview Header Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${data.format.label} • ${data.teamCount} Teams",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isFinalised) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = if (isFinalised) "FINALISED 🔒" else "${data.assignedCount} Active • ${data.substitutesCount} Subs",
                                                color = if (isFinalised) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Policy: ${data.subPolicy.label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (!isFinalised) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.generateTeams(matchId, SubstitutePolicy.STRICT_SUBSTITUTES) },
                                                modifier = Modifier.weight(1f),
                                                enabled = !state.isGenerating && data.subPolicy != SubstitutePolicy.STRICT_SUBSTITUTES
                                            ) {
                                                Text("Strict Subs", fontSize = 12.sp)
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.generateTeams(matchId, SubstitutePolicy.DISTRIBUTE_ALL) },
                                                modifier = Modifier.weight(1f),
                                                enabled = !state.isGenerating && data.subPolicy != SubstitutePolicy.DISTRIBUTE_ALL
                                            ) {
                                                Text("Distribute All", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Warnings Section
                        if (data.warnings.isNotEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        data.warnings.forEach { warning ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Warning,
                                                    contentDescription = "Warning",
                                                    tint = Color(0xFFE65100),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = warning,
                                                    color = Color(0xFFE65100),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Teams Cards
                        items(data.teams) { team ->
                            InteractiveTeamCard(
                                team = team,
                                selectedPlayerId = state.selectedPlayerForSwap?.id,
                                isFinalised = isFinalised,
                                onPlayerClick = { p -> viewModel.onPlayerClicked(p) },
                                onPlayerMoveClick = { p -> movingPlayer = p }
                            )
                        }

                        // Substitutes / Bench Section
                        if (data.substitutes.isNotEmpty()) {
                            item {
                                InteractiveSubstitutesCard(
                                    substitutes = data.substitutes,
                                    selectedPlayerId = state.selectedPlayerForSwap?.id,
                                    isFinalised = isFinalised,
                                    onPlayerClick = { p -> viewModel.onPlayerClicked(p) },
                                    onPlayerMoveClick = { p -> movingPlayer = p }
                                )
                            }
                        }

                        // Bottom Actions: Finalise & WhatsApp Export
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (!isFinalised) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = { showFinaliseDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Finalise Match Teams 🔒", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        copiedToClipboard = false
                                        showWhatsAppDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Export to WhatsApp 📲", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    // WhatsApp Export Preview Dialog
                    if (showWhatsAppDialog) {
                        val formattedText = remember(data) {
                            WhatsAppFormatter.format(data)
                        }
                        AlertDialog(
                            onDismissRequest = { showWhatsAppDialog = false },
                            title = { Text("WhatsApp Export Preview", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 320.dp)
                                    ) {
                                        Text(
                                            text = formattedText,
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .verticalScroll(rememberScrollState())
                                        )
                                    }
                                    if (copiedToClipboard) {
                                        Text(
                                            text = "✅ Copied to clipboard! Ready to paste in WhatsApp.",
                                            color = Color(0xFF2E7D32),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(formattedText))
                                        copiedToClipboard = true
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy")
                                }
                            },
                            dismissButton = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, formattedText)
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Teams via"))
                                        }
                                    ) {
                                        Text("Share")
                                    }
                                    TextButton(onClick = { showWhatsAppDialog = false }) {
                                        Text("Close")
                                    }
                                }
                            }
                        )
                    }

                    // Finalise Confirmation Dialog
                    if (showFinaliseDialog) {
                        AlertDialog(
                            onDismissRequest = { showFinaliseDialog = false },
                            title = { Text("Finalise Match Teams?") },
                            text = { Text("This will lock the current teams and player allocations. You can still export to WhatsApp directly after finalising.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showFinaliseDialog = false
                                        viewModel.finaliseMatch()
                                    }
                                ) {
                                    Text("Confirm Finalise")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showFinaliseDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // Move Player Dialog
                    if (movingPlayer != null) {
                        val p = movingPlayer!!
                        AlertDialog(
                            onDismissRequest = { movingPlayer = null },
                            title = { Text("Move ${p.displayName}") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Select a destination team or move to bench:", style = MaterialTheme.typography.bodyMedium)
                                    data.teams.forEach { team ->
                                        OutlinedButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                viewModel.movePlayer(p.id, team.id, isSubstitute = false)
                                                movingPlayer = null
                                            }
                                        ) {
                                            Text("Move to ${team.teamName} (${team.players.size} players)")
                                        }
                                    }
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            viewModel.movePlayer(p.id, null, isSubstitute = true)
                                            movingPlayer = null
                                        }
                                    ) {
                                        Text("Move to Bench / Substitutes")
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                TextButton(onClick = { movingPlayer = null }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveTeamCard(
    team: TeamDetail,
    selectedPlayerId: String?,
    isFinalised: Boolean,
    onPlayerClick: (TeamPlayerDetail) -> Unit,
    onPlayerMoveClick: (TeamPlayerDetail) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Team Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = team.teamName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    if (team.bibColor != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = team.bibColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${team.players.size} players",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Position Breakdown Pills
            val gkCount = team.players.count { it.assignedPosition == Position.KEEPER }
            val defCount = team.players.count { it.assignedPosition == Position.DEFENDER }
            val midCount = team.players.count { it.assignedPosition == Position.MIDFIELDER }
            val strCount = team.players.count { it.assignedPosition == Position.STRIKER }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PositionPillMini("GK", gkCount, positionColor(Position.KEEPER))
                PositionPillMini("DEF", defCount, positionColor(Position.DEFENDER))
                PositionPillMini("MID", midCount, positionColor(Position.MIDFIELDER))
                PositionPillMini("STR", strCount, positionColor(Position.STRIKER))
            }

            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Player List with interactive tap & move actions
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                team.players.forEach { p ->
                    val isSelected = p.id == selectedPlayerId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                else Modifier
                            )
                            .clickable(enabled = !isFinalised) { onPlayerClick(p) },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(positionColor(p.assignedPosition)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p.assignedPosition.name.take(1),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = p.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (p.isManualOverride) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "Manual",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (p.isEmergencyKeeper) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFFF3E0)
                                    ) {
                                        Text(
                                            text = "Emergency GK",
                                            color = Color(0xFFE65100),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                if (!isFinalised) {
                                    IconButton(
                                        onClick = { onPlayerMoveClick(p) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Move Options",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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
fun InteractiveSubstitutesCard(
    substitutes: List<TeamPlayerDetail>,
    selectedPlayerId: String?,
    isFinalised: Boolean,
    onPlayerClick: (TeamPlayerDetail) -> Unit,
    onPlayerMoveClick: (TeamPlayerDetail) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Substitutes / Bench",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${substitutes.size} subs",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            substitutes.forEach { s ->
                val isSelected = s.id == selectedPlayerId
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                        .clickable(enabled = !isFinalised) { onPlayerClick(s) },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(positionColor(s.primaryPosition)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s.primaryPosition.name.take(1),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = s.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (s.isManualOverride) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "Manual",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        if (!isFinalised) {
                            IconButton(
                                onClick = { onPlayerMoveClick(s) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Move Options",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
