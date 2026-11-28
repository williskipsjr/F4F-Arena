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
    onSignIn: (usernameOrPhone: String, password: String) -> Unit,
    onSignUp: (name: String, phone: String, age: Int, pin: String, firebaseIdToken: String?) -> Unit,
    onResetPin: (username: String, phone: String, newPin: String) -> Unit
) {
    var mode by remember(initialSignUp) {
        mutableStateOf(if (initialSignUp) AuthMode.SIGN_UP else AuthMode.SIGN_IN)
    }

    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }

    val phoneDigits = phone.filter { it.isDigit() }
    val parsedAge = ageText.toIntOrNull()

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
                    listOf(AuthMode.SIGN_IN to "Sign In", AuthMode.SIGN_UP to "Sign Up").forEach { (tabMode, label) ->
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
                        onValueChange = { username = it },
                        label = { Text("Phone Number or Username (or admin)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = secret,
                        onValueChange = { secret = it },
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
                        TextButton(onClick = { mode = AuthMode.RESET_PIN; secret = "" }) {
                            Text("Forgot PIN?", color = F4FColors.ForestMid, fontSize = 13.sp)
                        }
                    }
                }

                AuthMode.RESET_PIN -> {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Phone (10 digits) or Username") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Registered Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = secret,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) secret = it },
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
                        TextButton(onClick = { mode = AuthMode.SIGN_IN; secret = "" }) {
                            Text("Back to Sign In", color = F4FColors.ForestMid, fontSize = 13.sp)
                        }
                    }
                }

                AuthMode.SIGN_UP -> {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone (10 digits)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) ageText = it },
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
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) secret = it },
                        label = { Text("Set 4-Digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your playing position will be assigned by the Admin after sign-up.",
                        style = MaterialTheme.typography.labelMedium,
                        color = F4FColors.TextSecondary
                    )
                    Spacer(Modifier.height(14.dp))
                }
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
                AuthMode.RESET_PIN -> !isLoading && username.isNotBlank() && phoneDigits.length >= 10 && secret.length == 4
                AuthMode.SIGN_UP -> !isLoading && name.isNotBlank() && phoneDigits.length >= 10 && (parsedAge in 10..80) && secret.length == 4
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
                        AuthMode.RESET_PIN -> onResetPin(username.trim(), phone.trim(), secret)
                        AuthMode.SIGN_UP -> onSignUp(name.trim(), phone.trim(), parsedAge ?: 18, secret, null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
