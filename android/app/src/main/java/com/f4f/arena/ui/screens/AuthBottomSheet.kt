package com.f4f.arena.ui.screens

import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f4f.arena.data.auth.FirebasePhoneAuthHelper
import com.f4f.arena.ui.components.PrimaryLimePillButton
import com.f4f.arena.ui.theme.F4FColors

private enum class AuthMode { SIGN_IN, SIGN_UP, RESET_PIN }
private enum class SignUpStep { DETAILS, OTP, SET_PIN }

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
    val context = LocalContext.current
    val activity = context as? Activity

    var mode by remember(initialSignUp) {
        mutableStateOf(if (initialSignUp) AuthMode.SIGN_UP else AuthMode.SIGN_IN)
    }
    var signUpStep by remember { mutableStateOf(SignUpStep.DETAILS) }

    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var firebaseIdToken by remember { mutableStateOf<String?>(null) }
    var otpBusy by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
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
                        label = { Text("Username or Phone (10 digits)") },
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
                        onValueChange = { if (it.length <= 6) secret = it; localError = null },
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
                    when (signUpStep) {
                        SignUpStep.DETAILS -> {
                            Text(
                                "Step 1 of 3 · Your Details",
                                style = MaterialTheme.typography.labelMedium,
                                color = F4FColors.ForestMid
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it; localError = null },
                                label = { Text("Full Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it; localError = null },
                                    label = { Text("Phone (+91...)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Your playing position will be assigned by the Admin after sign-up.",
                                style = MaterialTheme.typography.labelMedium,
                                color = F4FColors.TextSecondary
                            )
                        }

                        SignUpStep.OTP -> {
                            Text(
                                "Step 2 of 3 · Verify Phone OTP",
                                style = MaterialTheme.typography.labelMedium,
                                color = F4FColors.ForestMid
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Enter the 6-digit SMS code sent to ${FirebasePhoneAuthHelper.normalizeE164(phone)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = F4FColors.TextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) otpCode = it; localError = null },
                                label = { Text("6-Digit OTP") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { signUpStep = SignUpStep.DETAILS; localError = null }) {
                                    Text("Edit Phone", color = F4FColors.TextSecondary, fontSize = 13.sp)
                                }
                                TextButton(
                                    enabled = !otpBusy && activity != null,
                                    onClick = {
                                        if (activity != null) {
                                            otpBusy = true
                                            localError = null
                                            FirebasePhoneAuthHelper.sendOtp(
                                                activity = activity,
                                                rawPhone = phone,
                                                onCodeSent = { vid ->
                                                    verificationId = vid
                                                    otpBusy = false
                                                },
                                                onAutoVerifiedIdToken = { tok ->
                                                    firebaseIdToken = tok
                                                    otpBusy = false
                                                    signUpStep = SignUpStep.SET_PIN
                                                },
                                                onError = { err ->
                                                    otpBusy = false
                                                    localError = err
                                                }
                                            )
                                        }
                                    }
                                ) {
                                    Text("Resend OTP", color = F4FColors.ForestMid, fontSize = 13.sp)
                                }
                            }
                        }

                        SignUpStep.SET_PIN -> {
                            Text(
                                "Step 3 of 3 · Set Your 4-Digit PIN",
                                style = MaterialTheme.typography.labelMedium,
                                color = F4FColors.ForestMid
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Phone verified! Choose a 4-digit PIN for quick sign-in.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = F4FColors.TextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = secret,
                                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) secret = it; localError = null },
                                label = { Text("4-Digit PIN") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
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

            val busy = isLoading || otpBusy
            val canSubmit = when (mode) {
                AuthMode.SIGN_IN -> !busy && username.isNotBlank() && secret.length >= 4
                AuthMode.RESET_PIN -> !busy && username.isNotBlank() && phoneDigits.length >= 10 && secret.length >= 4
                AuthMode.SIGN_UP -> when (signUpStep) {
                    SignUpStep.DETAILS -> !busy && name.isNotBlank() && phoneDigits.length >= 10 && (parsedAge in 10..80)
                    SignUpStep.OTP -> !busy && otpCode.length == 6 && verificationId != null
                    SignUpStep.SET_PIN -> !busy && secret.length == 4
                }
            }

            PrimaryLimePillButton(
                text = when {
                    busy -> "Please wait..."
                    mode == AuthMode.SIGN_IN -> "Sign In"
                    mode == AuthMode.RESET_PIN -> "Reset PIN"
                    signUpStep == SignUpStep.DETAILS -> "Send OTP"
                    signUpStep == SignUpStep.OTP -> "Verify OTP"
                    else -> "Set PIN & Continue"
                },
                enabled = canSubmit,
                onClick = {
                    localError = null
                    when (mode) {
                        AuthMode.SIGN_IN -> onSignIn(username.trim(), secret)
                        AuthMode.RESET_PIN -> onResetPin(username.trim(), phone.trim(), secret)
                        AuthMode.SIGN_UP -> when (signUpStep) {
                            SignUpStep.DETAILS -> {
                                if (activity == null) {
                                    localError = "Unable to start phone verification"
                                } else {
                                    otpBusy = true
                                    FirebasePhoneAuthHelper.sendOtp(
                                        activity = activity,
                                        rawPhone = phone,
                                        onCodeSent = { vid ->
                                            verificationId = vid
                                            otpBusy = false
                                            signUpStep = SignUpStep.OTP
                                        },
                                        onAutoVerifiedIdToken = { tok ->
                                            firebaseIdToken = tok
                                            otpBusy = false
                                            signUpStep = SignUpStep.SET_PIN
                                        },
                                        onError = { err ->
                                            otpBusy = false
                                            localError = err
                                        }
                                    )
                                }
                            }
                            SignUpStep.OTP -> {
                                val vid = verificationId ?: return@PrimaryLimePillButton
                                otpBusy = true
                                FirebasePhoneAuthHelper.verifyOtp(
                                    context = context,
                                    verificationId = vid,
                                    code = otpCode,
                                    onVerifiedIdToken = { tok ->
                                        firebaseIdToken = tok
                                        otpBusy = false
                                        signUpStep = SignUpStep.SET_PIN
                                    },
                                    onError = { err ->
                                        otpBusy = false
                                        localError = err
                                    }
                                )
                            }
                            SignUpStep.SET_PIN -> {
                                val age = parsedAge ?: 18
                                onSignUp(name.trim(), phone.trim(), age, secret, firebaseIdToken)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
