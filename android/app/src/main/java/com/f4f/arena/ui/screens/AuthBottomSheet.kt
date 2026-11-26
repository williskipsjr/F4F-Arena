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
import com.f4f.arena.ui.components.PrimaryLimePillButton
import com.f4f.arena.ui.theme.F4FColors

private enum class AuthMode { SIGN_IN, SIGN_UP, RESET_PIN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBottomSheet(
    initialSignUp: Boolean = false,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSignIn: (username: String, password: String) -> Unit,
    onSignUp: (name: String, username: String, phone: String, pin: String) -> Unit,
    onResetPin: (username: String, phone: String, newPin: String) -> Unit
) {
    var mode by remember(initialSignUp) {
        mutableStateOf(if (initialSignUp) AuthMode.SIGN_UP else AuthMode.SIGN_IN)
    }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }

    val phoneDigits = phone.filter { it.isDigit() }

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
                    "Verify your username and registered phone number to set a new 4-digit PIN.",
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
                    listOf(AuthMode.SIGN_IN to "Sign In", AuthMode.SIGN_UP to "Sign Up").forEach { (tabMode, label) ->
                        val selected = mode == tabMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) F4FColors.ForestDeep else Color.Transparent)
                                .clickable { mode = tabMode },
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

            if (mode == AuthMode.SIGN_UP) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Full Name (e.g., Lenpu)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(if (mode == AuthMode.SIGN_IN) "Username (or admin)" else "Username (e.g. lenpu)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (mode == AuthMode.SIGN_UP || mode == AuthMode.RESET_PIN) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Required · 10+ digits)") },
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
                label = {
                    Text(
                        when (mode) {
                            AuthMode.SIGN_UP -> "4-Digit PIN (min 4 chars)"
                            AuthMode.RESET_PIN -> "New 4-Digit PIN (min 4 chars)"
                            AuthMode.SIGN_IN -> "PIN / Password"
                        }
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (mode == AuthMode.SIGN_IN) KeyboardType.Text else KeyboardType.NumberPassword
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (mode == AuthMode.SIGN_IN) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { mode = AuthMode.RESET_PIN; secret = "" }) {
                        Text("Forgot PIN?", color = F4FColors.ForestMid, fontSize = 13.sp)
                    }
                }
            } else if (mode == AuthMode.RESET_PIN) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { mode = AuthMode.SIGN_IN; secret = "" }) {
                        Text("Back to Sign In", color = F4FColors.ForestMid, fontSize = 13.sp)
                    }
                }
            } else {
                Spacer(Modifier.height(14.dp))
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = F4FColors.DangerRed,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
            }

            val canSubmit = when (mode) {
                AuthMode.SIGN_IN -> !isLoading && username.isNotBlank() && secret.length >= 4
                AuthMode.SIGN_UP -> !isLoading && name.isNotBlank() && username.isNotBlank() && phoneDigits.length >= 10 && secret.length >= 4
                AuthMode.RESET_PIN -> !isLoading && username.isNotBlank() && phoneDigits.length >= 10 && secret.length >= 4
            }

            PrimaryLimePillButton(
                text = when {
                    isLoading -> "Please wait..."
                    mode == AuthMode.SIGN_UP -> "Create Account"
                    mode == AuthMode.RESET_PIN -> "Reset PIN"
                    else -> "Sign In"
                },
                enabled = canSubmit,
                onClick = {
                    when (mode) {
                        AuthMode.SIGN_IN -> onSignIn(username.trim(), secret)
                        AuthMode.SIGN_UP -> onSignUp(name.trim(), username.trim(), phone.trim(), secret)
                        AuthMode.RESET_PIN -> onResetPin(username.trim(), phone.trim(), secret)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
