package com.f4f.arena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.f4f.arena.ui.HealthScreen
import com.f4f.arena.ui.match.AttendanceScreen
import com.f4f.arena.ui.match.MatchSetupScreen
import com.f4f.arena.ui.roster.RosterScreen
import com.f4f.arena.ui.theme.F4FArenaTheme

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

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentDestination == AppDestination.MATCHES,
                                onClick = {
                                    currentDestination = AppDestination.MATCHES
                                    activeAttendanceMatchId = null
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentDestination) {
                            AppDestination.MATCHES -> {
                                if (activeAttendanceMatchId == null) {
                                    MatchSetupScreen(
                                        onOpenAttendance = { matchId ->
                                            activeAttendanceMatchId = matchId
                                        }
                                    )
                                } else {
                                    AttendanceScreen(
                                        matchId = activeAttendanceMatchId!!,
                                        onBack = { activeAttendanceMatchId = null },
                                        onProceedToMatchmaking = { _ ->
                                            // Phase 3 ready
                                        }
                                    )
                                }
                            }
                            AppDestination.ROSTER -> RosterScreen()
                            AppDestination.HEALTH -> HealthScreen()
                        }
                    }
                }
            }
        }
    }
}
