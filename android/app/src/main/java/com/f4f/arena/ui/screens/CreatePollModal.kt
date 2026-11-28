package com.f4f.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.f4f.arena.ui.components.PrimaryLimePillButton
import com.f4f.arena.ui.state.MatchFormat
import com.f4f.arena.ui.theme.F4FColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePollModal(
    isCreating: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onCreatePoll: (title: String, kickoffTime: String, venue: String, format: MatchFormat, openPoll: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("Match tonight") }
    var kickoffTime by remember { mutableStateOf("10:00 PM") }
    var venue by remember { mutableStateOf("Turf Ground") }
    var format by remember { mutableStateOf(MatchFormat.SEVEN_A_SIDE) }
    var openPoll by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = { if (!isCreating) onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Create Match Poll", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Match Title") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = kickoffTime,
                    onValueChange = { kickoffTime = it },
                    label = { Text("Kickoff Time") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Format", style = MaterialTheme.typography.labelMedium, color = F4FColors.TextSecondary)
            Spacer(Modifier.height(8.dp))

            MatchFormat.entries.forEach { option ->
                val selected = option == format
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) Color(0xFFECFDF3) else F4FColors.SurfaceMuted)
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) F4FColors.PitchGreen else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { format = option }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected, onClick = { format = option })
                    Column {
                        Text(option.label, fontWeight = FontWeight.Bold)
                        Text(option.formation, style = MaterialTheme.typography.labelMedium, color = F4FColors.TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Open IN/OUT poll for players", style = MaterialTheme.typography.titleMedium)
                Switch(checked = openPoll, onCheckedChange = { openPoll = it })
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(errorMessage, color = F4FColors.DangerRed, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isCreating,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }
                PrimaryLimePillButton(
                    text = if (isCreating) "Creating..." else "Create Poll",
                    enabled = !isCreating && title.isNotBlank() && venue.isNotBlank(),
                    onClick = { onCreatePoll(title.trim(), kickoffTime.trim(), venue.trim(), format, openPoll) },
                    modifier = Modifier.weight(1.3f)
                )
            }
        }
    }
}
