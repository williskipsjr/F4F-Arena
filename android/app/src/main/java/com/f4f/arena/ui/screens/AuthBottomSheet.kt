package com.f4f.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.f4f.arena.data.auth.SessionManager
import com.f4f.arena.data.model.PhoneLookupResponse
import com.f4f.arena.ui.components.PrimaryLimePillButton
import com.f4f.arena.ui.theme.F4FColors
import kotlinx.coroutines.launch

private enum class AuthMode { SIGN_IN, SIGN_UP, RESET_PIN }
private enum class PhoneStage { ENTER_PHONE, FOUND_CLAIM, FOUND_LOGIN, NEW_PLAYER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBottomSheet(
    initialSignUp: Boolean = false,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSignIn: (usernameOrPhone: String, password: String) -> Unit,
    onClaimProfile: (playerId: String, phone: String, pin: String) -> Unit,
    onSignUp: (name: String, phone: String, age: Int, pin: String, firebaseIdToken: String?) -> Unit,
    onResetPin: (username: String, phone: String, newPin: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var mode by remember(initialSignUp) {
        mutableStateOf(if (initialSignUp) AuthMode.SIGN_UP else AuthMode.SIGN_IN)
    }
    var phoneStage by remember { mutableStateOf(PhoneStage.ENTER_PHONE) }
    var lookupResult by remember { mutableStateOf<PhoneLookupResponse?>(null) }
    var lookupLoading by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }

    val phoneDigits = phone.filter { it.isDigit() }
    val parsedAge = ageText.toIntOrNull()
    val displayError = localError ?: errorMessage

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
            if (mode == AuthMode.RESET_PIN) {
                Text("Reset Forgotten PIN", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Verify your phone/username and registered phone number to set a new 4-digit PIN.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = F4FColors.TextSecondary
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(F4FColors.SurfaceMuted)
                        .padding(4.dp)
                ) {
                    listOf(AuthMode.SIGN_IN to "Sign In", AuthMode.SIGN_UP to "Get Started").forEach { (tabMode, label) ->
                        val selected = mode == tabMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) F4FColors.ForestDeep else Color.Transparent)
                                .clickable {
                                    mode = tabMode
                                    secret = ""
                                    localError = null
                                },
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
            }

            Spacer(Modifier.height(18.dp))

            when (mode) {
                AuthMode.SIGN_IN -> {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; localError = null },
                        label = { Text("Phone Number or Username (or admin)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = secret,
                        onValueChange = { secret = it; localError = null },
                        label = { Text("4-Digit PIN / Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { mode = AuthMode.RESET_PIN; secret = ""; localError = null }) {
                            Text("Forgot PIN?", color = F4FColors.ForestMid, fontSize = 13.sp)
                        }
                    }
                }

                AuthMode.RESET_PIN -> {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; localError = null },
                        label = { Text("Phone (10 digits) or Username") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; localError = null },
                        label = { Text("Registered Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = secret,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) secret = it; localError = null },
                        label = { Text("New 4-Digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { mode = AuthMode.SIGN_IN; secret = ""; localError = null }) {
                            Text("Back to Sign In", color = F4FColors.ForestMid, fontSize = 13.sp)
                        }
                    }
                }

                AuthMode.SIGN_UP -> {
                    when (phoneStage) {
                        PhoneStage.ENTER_PHONE -> {
                            Text(
                                "Enter your registered phone number to find your F4F Arena profile.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = F4FColors.TextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it; localError = null },
                                label = { Text("Phone Number (10 digits)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        PhoneStage.FOUND_CLAIM, PhoneStage.FOUND_LOGIN -> {
                            val playerName = lookupResult?.displayName ?: "Player"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFDCFCE7))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = if (phoneStage == PhoneStage.FOUND_CLAIM) {
                                            "Welcome, $playerName! 👋"
                                        } else {
                                            "Welcome back, $playerName! 👋"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = F4FColors.ForestDeep,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (phoneStage == PhoneStage.FOUND_CLAIM) {
                                            "We found your profile in the squad. Set a 4-digit PIN to continue."
                                        } else {
                                            "Enter your 4-digit PIN to sign in."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = F4FColors.ForestMid
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = secret,
                                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) secret = it; localError = null },
                                label = {
                                    Text(
                                        if (phoneStage == PhoneStage.FOUND_CLAIM) "Set 4-Digit PIN" else "Enter 4-Digit PIN"
                                    )
                                },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                TextButton(onClick = { phoneStage = PhoneStage.ENTER_PHONE; secret = ""; localError = null }) {
                                    Text("Change Phone", color = F4FColors.TextSecondary, fontSize = 13.sp)
                                }
                            }
                        }

                        PhoneStage.NEW_PLAYER -> {
                            Text(
                                "Phone not in our pre-seeded list yet — quick sign up:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = F4FColors.TextSecondary
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it; localError = null },
                                    label = { Text("Full Name") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(2f)
                                )
                                OutlinedTextField(
                                    value = ageText,
                                    onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) ageText = it; localError = null },
                                    label = { Text("Age") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = secret,
                                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) secret = it; localError = null },
                                label = { Text("Set 4-Digit PIN") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                TextButton(onClick = { phoneStage = PhoneStage.ENTER_PHONE; secret = ""; localError = null }) {
                                    Text("Change Phone", color = F4FColors.TextSecondary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            if (!displayError.isNullOrBlank()) {
                Text(
                    text = displayError,
                    color = F4FColors.DangerRed,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
            }

            val busy = isLoading || lookupLoading
            val canSubmit = when (mode) {
                AuthMode.SIGN_IN -> !busy && username.isNotBlank() && secret.length >= 4
                AuthMode.RESET_PIN -> !busy && username.isNotBlank() && phoneDigits.length >= 10 && secret.length == 4
                AuthMode.SIGN_UP -> when (phoneStage) {
                    PhoneStage.ENTER_PHONE -> !busy && phoneDigits.length >= 10
                    PhoneStage.FOUND_CLAIM, PhoneStage.FOUND_LOGIN -> !busy && secret.length == 4
                    PhoneStage.NEW_PLAYER -> !busy && name.isNotBlank() && (parsedAge in 10..80) && secret.length == 4
                }
            }

            PrimaryLimePillButton(
                text = when {
                    busy -> "Please wait..."
                    mode == AuthMode.SIGN_IN -> "Sign In"
                    mode == AuthMode.RESET_PIN -> "Reset PIN"
                    phoneStage == PhoneStage.ENTER_PHONE -> "Continue"
                    phoneStage == PhoneStage.FOUND_CLAIM -> "Claim & Enter"
                    phoneStage == PhoneStage.FOUND_LOGIN -> "Sign In"
                    else -> "Create Account"
                },
                enabled = canSubmit,
                onClick = {
                    localError = null
                    when (mode) {
                        AuthMode.SIGN_IN -> onSignIn(username.trim(), secret)
                        AuthMode.RESET_PIN -> onResetPin(username.trim(), phone.trim(), secret)
                        AuthMode.SIGN_UP -> when (phoneStage) {
                            PhoneStage.ENTER_PHONE -> {
                                scope.launch {
                                    lookupLoading = true
                                    val res = SessionManager.phoneLookup(phone)
                                    lookupLoading = false
                                    res.onSuccess { found ->
                                        lookupResult = found
                                        secret = ""
                                        phoneStage = when {
                                            found.found && !found.claimed -> PhoneStage.FOUND_CLAIM
                                            found.found && found.claimed -> PhoneStage.FOUND_LOGIN
                                            else -> PhoneStage.NEW_PLAYER
                                        }
                                    }.onFailure { err ->
                                        localError = err.localizedMessage ?: "Lookup failed"
                                    }
                                }
                            }
                            PhoneStage.FOUND_CLAIM -> {
                                val pid = lookupResult?.playerId ?: return@PrimaryLimePillButton
                                onClaimProfile(pid, phone.trim(), secret)
                            }
                            PhoneStage.FOUND_LOGIN -> {
                                val last10 = phoneDigits.takeLast(10)
                                onSignIn(last10, secret)
                            }
                            PhoneStage.NEW_PLAYER -> {
                                onSignUp(name.trim(), phone.trim(), parsedAge ?: 18, secret, null)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
