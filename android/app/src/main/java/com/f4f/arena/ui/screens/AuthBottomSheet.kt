package com.f4f.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f4f.arena.ui.components.PrimaryLimePillButton
import com.f4f.arena.ui.state.PlayerUi
import com.f4f.arena.ui.theme.F4FColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBottomSheet(
    initialSignUp: Boolean = false,
    rosterPlayers: List<PlayerUi> = emptyList(),
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSignIn: (username: String, password: String) -> Unit,
    onSignUp: (name: String, username: String, phone: String?, pin: String, claimedPlayerId: String?) -> Unit
) {
    var isSignUp by remember(initialSignUp) { mutableStateOf(initialSignUp) }
    var selectedClaimPlayer by remember { mutableStateOf<PlayerUi?>(null) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }

    val matchingRoster = remember(name, rosterPlayers) {
        if (name.isBlank()) rosterPlayers.take(8)
        else rosterPlayers.filter { it.name.contains(name, ignoreCase = true) }.take(8)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(F4FColors.SurfaceMuted)
                    .padding(4.dp)
            ) {
                listOf(false to "Sign In", true to "Claim / Sign Up").forEach { (tabSignUp, label) ->
                    val selected = isSignUp == tabSignUp
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) F4FColors.ForestDeep else Color.Transparent)
                            .clickable { isSignUp = tabSignUp },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else F4FColors.TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (isSignUp) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (selectedClaimPlayer != null && selectedClaimPlayer!!.name != it) {
                            selectedClaimPlayer = null
                        }
                    },
                    label = { Text("Search your name or enter new") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (matchingRoster.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (selectedClaimPlayer != null) "Claiming: ${selectedClaimPlayer!!.name} (${selectedClaimPlayer!!.position.shortCode})"
                        else "Tap your name if you're already in the roster:",
                        style = MaterialTheme.typography.labelMedium,
                        color = F4FColors.ForestMid
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(matchingRoster, key = { it.id }) { player ->
                            val isSelected = selectedClaimPlayer?.id == player.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) F4FColors.ForestDeep else F4FColors.SurfaceMuted)
                                    .border(
                                        1.dp,
                                        if (isSelected) F4FColors.PitchLime else Color.Transparent,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        selectedClaimPlayer = player
                                        name = player.name
                                        if (username.isBlank()) {
                                            username = player.name.lowercase().replace(" ", "_")
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${player.name} · ${player.position.shortCode}",
                                    color = if (isSelected) Color.White else F4FColors.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(if (isSignUp) "Username (e.g. lenpu)" else "Username (or admin)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (isSignUp) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = { Text(if (isSignUp) "4-Digit PIN (or Password)" else "PIN / Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isSignUp) KeyboardType.NumberPassword else KeyboardType.Text
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = errorMessage,
                    color = F4FColors.DangerRed,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(20.dp))

            PrimaryLimePillButton(
                text = if (isLoading) "Please wait..." else if (isSignUp) "Complete Setup" else "Sign In",
                enabled = !isLoading && username.isNotBlank() && secret.length >= 4 && (!isSignUp || name.isNotBlank()),
                onClick = {
                    if (isSignUp) {
                        onSignUp(
                            name.trim(),
                            username.trim(),
                            phone.trim().ifEmpty { null },
                            secret,
                            selectedClaimPlayer?.id
                        )
                    } else {
                        onSignIn(username.trim(), secret)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
