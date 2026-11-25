package com.f4f.arena

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.f4f.arena.data.auth.SessionManager
import com.f4f.arena.ui.HealthScreen
import com.f4f.arena.ui.match.AttendanceScreen
import com.f4f.arena.ui.match.MatchSetupScreen
import com.f4f.arena.ui.match.TeamBoardScreen
import com.f4f.arena.ui.roster.RosterScreen
import com.f4f.arena.ui.theme.F4FArenaTheme
import kotlinx.coroutines.launch

enum class AppDestination(val label: String) {
    MATCHES("Matches"),
    ROSTER("Roster"),
    HEALTH("Health")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            F4FArenaTheme {
                var currentDestination by remember { mutableStateOf(AppDestination.MATCHES) }
                var activeAttendanceMatchId by remember { mutableStateOf<String?>(null) }
                var activeTeamBoardMatchId by remember { mutableStateOf<String?>(null) }
                var showAuthDialog by remember { mutableStateOf(false) }

                val context = LocalContext.current
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val token by SessionManager.token.collectAsState()
                val displayName by SessionManager.displayName.collectAsState()
                val adminUser by SessionManager.adminUsername.collectAsState()
                val role by SessionManager.role.collectAsState()
                val authPromptRequired by SessionManager.authPromptRequired.collectAsState()
                val isLoggedIn = !token.isNullOrBlank()
                val isAdmin = isLoggedIn && role == "ADMIN"

                LaunchedEffect(authPromptRequired) {
                    if (authPromptRequired) {
                        showAuthDialog = true
                        SessionManager.consumeAuthPrompt()
                    }
                }

                BackHandler(
                    enabled = currentDestination == AppDestination.MATCHES &&
                        (activeTeamBoardMatchId != null || activeAttendanceMatchId != null)
                ) {
                    when {
                        activeTeamBoardMatchId != null -> activeTeamBoardMatchId = null
                        activeAttendanceMatchId != null -> activeAttendanceMatchId = null
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentDestination == AppDestination.MATCHES,
                                onClick = {
                                    currentDestination = AppDestination.MATCHES
                                    activeAttendanceMatchId = null
                                    activeTeamBoardMatchId = null
                                },
                                icon = { Icon(Icons.Default.SportsSoccer, contentDescription = "Matches") },
                                label = { Text("Matches") }
                            )
                            NavigationBarItem(
                                selected = currentDestination == AppDestination.ROSTER,
                                onClick = { currentDestination = AppDestination.ROSTER },
                                icon = { Icon(Icons.Default.People, contentDescription = "Roster") },
                                label = { Text("Roster") }
                            )
                            NavigationBarItem(
                                selected = currentDestination == AppDestination.HEALTH,
                                onClick = { currentDestination = AppDestination.HEALTH },
                                icon = { Icon(Icons.Default.Info, contentDescription = "Health") },
                                label = { Text("System") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Session Status Bar
                        Surface(
                            color = when {
                                isAdmin -> MaterialTheme.colorScheme.primaryContainer
                                isLoggedIn -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        when {
                                            isAdmin -> Icons.Default.VerifiedUser
                                            isLoggedIn -> Icons.Default.Person
                                            else -> Icons.Default.Lock
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            isAdmin -> "Admin: ${displayName ?: adminUser ?: "Signed In"}"
                                            isLoggedIn -> "Player: ${displayName ?: adminUser ?: "Signed In"}"
                                            else -> "Guest (Sign in / Sign up to vote)"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        if (isLoggedIn) SessionManager.logout() else showAuthDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(if (isLoggedIn) "Sign Out" else "Sign In / Up")
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (currentDestination) {
                                AppDestination.MATCHES -> {
                                    when {
                                        activeTeamBoardMatchId != null -> {
                                            TeamBoardScreen(
                                                matchId = activeTeamBoardMatchId!!,
                                                isAdmin = isAdmin,
                                                onBack = { activeTeamBoardMatchId = null }
                                            )
                                        }
                                        activeAttendanceMatchId != null -> {
                                            AttendanceScreen(
                                                matchId = activeAttendanceMatchId!!,
                                                onBack = { activeAttendanceMatchId = null },
                                                onProceedToMatchmaking = { matchId ->
                                                    activeTeamBoardMatchId = matchId
                                                }
                                            )
                                        }
                                        else -> {
                                            MatchSetupScreen(
                                                isAdmin = isAdmin,
                                                isLoggedIn = isLoggedIn,
                                                onRequireAuth = { showAuthDialog = true },
                                                onOpenAttendance = { matchId ->
                                                    activeAttendanceMatchId = matchId
                                                },
                                                onOpenTeams = { matchId ->
                                                    activeTeamBoardMatchId = matchId
                                                }
                                            )
                                        }
                                    }
                                }
                                AppDestination.ROSTER -> RosterScreen(isAdmin = isAdmin)
                                AppDestination.HEALTH -> HealthScreen()
                            }
                        }
                    }
                }

                if (showAuthDialog) {
                    AuthDialog(
                        onDismiss = { showAuthDialog = false },
                        onSuccess = { showAuthDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun AuthDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isSignUp) "Player Sign Up" else "Sign In",
                    fontWeight = FontWeight.Bold
                )
                TabRow(selectedTabIndex = if (isSignUp) 1 else 0) {
                    Tab(
                        selected = !isSignUp,
                        onClick = { isSignUp = false; errorText = null },
                        text = { Text("Sign In") }
                    )
                    Tab(
                        selected = isSignUp,
                        onClick = { isSignUp = true; errorText = null },
                        text = { Text("Sign Up") }
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isSignUp) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Your Name (e.g. Lenpu)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(if (isSignUp) "Username (for login)" else "Username (or admin)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isSignUp) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isSignUp) "PIN / Password (min 4 chars)" else "Password / Passphrase") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            val canSubmit = if (isSignUp) {
                !isLoading && displayName.isNotBlank() && username.isNotBlank() && password.length >= 4
            } else {
                !isLoading && username.isNotBlank() && password.isNotBlank()
            }
            Button(
                enabled = canSubmit,
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorText = null
                        val res = if (isSignUp) {
                            SessionManager.register(displayName, username, phone, password)
                        } else {
                            SessionManager.login(username, password)
                        }
                        isLoading = false
                        if (res.isSuccess) {
                            onSuccess()
                        } else {
                            errorText = res.exceptionOrNull()?.message ?: "Authentication failed"
                        }
                    }
                }
            ) {
                Text(
                    when {
                        isLoading -> "Please wait..."
                        isSignUp -> "Create Account"
                        else -> "Sign In"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
