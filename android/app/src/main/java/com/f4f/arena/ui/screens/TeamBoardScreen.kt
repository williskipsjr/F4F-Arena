package com.f4f.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f4f.arena.ui.components.PrimaryLimePillButton
import com.f4f.arena.ui.state.*
import com.f4f.arena.ui.theme.F4FColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamBoardScreen(
    format: MatchFormat,
    policy: TeamPolicy,
    teams: List<GeneratedTeamUi>,
    bench: List<TeamSlotUi>,
    isAdmin: Boolean,
    isFinalised: Boolean,
    isGenerating: Boolean = false,
    swapSourcePlayerId: String?,
    onBack: () -> Unit,
    onPolicyChange: (TeamPolicy) -> Unit,
    onFormatChange: (MatchFormat) -> Unit = {},
    onAutoFormat: () -> Unit = {},
    onReshuffle: () -> Unit,
    onTapPlayerForSwap: (String) -> Unit,
    onMovePlayer: (playerId: String, targetTeamId: String?) -> Unit,
    onToggleFinalise: () -> Unit,
    onOpenExport: () -> Unit
) {
    val activeCount = teams.sumOf { it.players.size }

    Scaffold(
        containerColor = F4FColors.CanvasOffWhite,
        topBar = {
            TopAppBar(
                title = { Text("TEAM BOARD", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenExport) {
                        Icon(Icons.Outlined.Share, contentDescription = "Export")
                    }
                    if (isAdmin && !isFinalised) {
                        IconButton(onClick = onReshuffle) {
                            Icon(Icons.Outlined.Shuffle, contentDescription = "Reshuffle")
                        }
                    }
                    if (isAdmin) {
                        IconButton(onClick = onToggleFinalise) {
                            Icon(
                                if (isFinalised) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                contentDescription = "Finalise"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = F4FColors.CanvasOffWhite)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(22.dp), color = Color.White) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${format.label} · ${teams.size} Teams",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                if (isFinalised) "FINALISED" else "$activeCount Active",
                                style = MaterialTheme.typography.labelMedium,
                                color = F4FColors.ForestMid
                            )
                        }

                        if (isAdmin && !isFinalised) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Format (Auto picks even teams)",
                                style = MaterialTheme.typography.labelMedium,
                                color = F4FColors.TextSecondary
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(F4FColors.PitchLime.copy(alpha = 0.22f))
                                        .clickable(enabled = !isGenerating) { onAutoFormat() }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Auto",
                                        color = F4FColors.ForestDeep,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                MatchFormat.entries.forEach { fmt ->
                                    val selected = fmt == format
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (selected) F4FColors.ForestDeep else F4FColors.SurfaceMuted)
                                            .clickable(enabled = !isGenerating) { onFormatChange(fmt) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${fmt.size}v${fmt.size}",
                                            color = if (selected) F4FColors.PitchLime else F4FColors.TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    TeamPolicy.STRICT_SUBS to "Strict Subs",
                                    TeamPolicy.DISTRIBUTE_ALL to "Dist. All"
                                ).forEach { (p, label) ->
                                    val selected = p == policy
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (selected) F4FColors.ForestDeep else F4FColors.SurfaceMuted)
                                            .clickable { onPolicyChange(p) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            label,
                                            color = if (selected) Color.White else F4FColors.TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            PrimaryLimePillButton(
                                text = if (isGenerating) "Reshuffling..." else "Reshuffle / Generate Again",
                                icon = Icons.Outlined.Shuffle,
                                enabled = !isGenerating,
                                onClick = onReshuffle,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            teams.filter { it.needsEmergencyGk }.forEach { team ->
                item(key = "warn_${team.id}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(F4FColors.WarningBg)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = F4FColors.WarningText)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${team.name} needs Emergency GK",
                            color = F4FColors.WarningText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            items(teams, key = { it.id }) { team ->
                Surface(shape = RoundedCornerShape(22.dp), color = Color.White) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(team.kitColorHex))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${team.name} · ${team.kitColorName}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                "${team.players.size} players",
                                style = MaterialTheme.typography.labelMedium,
                                color = F4FColors.TextSecondary
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = F4FColors.HairlineBorder)

                        team.players.forEach { slot ->
                            val isSelectedForSwap = swapSourcePlayerId == slot.playerId
                            var menuOpen by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelectedForSwap) Color(0xFFECFDF3) else Color.Transparent)
                                    .clickable(enabled = isAdmin && !isFinalised) {
                                        onTapPlayerForSwap(slot.playerId)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    slot.position.shortCode,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = F4FColors.ForestMid,
                                    modifier = Modifier.width(38.dp)
                                )
                                Text(
                                    slot.name + if (slot.isManual) " · Manual" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isAdmin && !isFinalised) {
                                    Box {
                                        IconButton(
                                            onClick = { menuOpen = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Outlined.MoreVert, contentDescription = "Move player")
                                        }
                                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                            teams.filter { it.id != team.id }.forEach { target ->
                                                DropdownMenuItem(
                                                    text = { Text("Move to ${target.name}") },
                                                    onClick = {
                                                        menuOpen = false
                                                        onMovePlayer(slot.playerId, target.id)
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Move to Bench") },
                                                onClick = {
                                                    menuOpen = false
                                                    onMovePlayer(slot.playerId, null)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (bench.isNotEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(22.dp), color = Color.White) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Substitutes / Bench · ${bench.size}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            bench.forEach { slot ->
                                val isSelectedForSwap = swapSourcePlayerId == slot.playerId
                                var menuOpen by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelectedForSwap) Color(0xFFECFDF3) else Color.Transparent)
                                        .clickable(enabled = isAdmin && !isFinalised) {
                                            onTapPlayerForSwap(slot.playerId)
                                        }
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        slot.position.shortCode,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = F4FColors.ForestMid,
                                        modifier = Modifier.width(38.dp)
                                    )
                                    Text(
                                        slot.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isAdmin && !isFinalised) {
                                        Box {
                                            IconButton(
                                                onClick = { menuOpen = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Outlined.MoreVert, contentDescription = "Move player")
                                            }
                                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                                teams.forEach { target ->
                                                    DropdownMenuItem(
                                                        text = { Text("Move to ${target.name}") },
                                                        onClick = {
                                                            menuOpen = false
                                                            onMovePlayer(slot.playerId, target.id)
                                                        }
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

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isAdmin && !isFinalised) {
                        PrimaryLimePillButton(
                            text = "Finalise Match Teams",
                            icon = Icons.Outlined.Lock,
                            onClick = onToggleFinalise,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenExport,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export to WhatsApp", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsAppExportDialog(
    formattedText: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = { Text("WhatsApp Lineup Export", fontWeight = FontWeight.Bold) },
        text = {
            Surface(
                color = F4FColors.SurfaceMuted,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                Text(
                    text = formattedText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = F4FColors.ForestDeep)
            ) {
                Text("Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onCopy) { Text("Copy") }
        }
    )
}
