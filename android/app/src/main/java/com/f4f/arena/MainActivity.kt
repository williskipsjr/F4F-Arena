package com.f4f.arena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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
                var showLoginDialog by remember { mutableStateOf(false) }

                val token by SessionManager.token.collectAsState()
                val adminUser by SessionManager.adminUsername.collectAsState()
                val isLoggedIn = !token.isNullOrBlank()

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
                        // Compact Admin Session Status Bar
                        Surface(
                            color = if (isLoggedIn) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer,
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
                                        if (isLoggedIn) Icons.Default.VerifiedUser else Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isLoggedIn) "Admin: ${adminUser ?: "Signed In"}" else "Read-only mode (Sign in to edit)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        if (isLoggedIn) SessionManager.logout() else showLoginDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(if (isLoggedIn) "Sign Out" else "Admin Login")
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
                                AppDestination.ROSTER -> RosterScreen()
                                AppDestination.HEALTH -> HealthScreen()
                            }
                        }
                    }
                }

                if (showLoginDialog) {
                    AdminLoginDialog(
                        onDismiss = { showLoginDialog = false },
                        onSuccess = { showLoginDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminLoginDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin Sign In", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Passphrase") },
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
            Button(
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorText = null
                        val res = SessionManager.login(username, password)
                        isLoading = false
                        if (res.isSuccess) {
                            onSuccess()
                        } else {
                            errorText = res.exceptionOrNull()?.message ?: "Login failed"
                        }
                    }
                }
            ) {
                Text(if (isLoading) "Signing in..." else "Sign In")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
