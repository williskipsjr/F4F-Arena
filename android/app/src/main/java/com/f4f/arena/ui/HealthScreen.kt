package com.f4f.arena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f4f.arena.data.api.NetworkClient
import com.f4f.arena.data.model.HealthResponse
import com.f4f.arena.ui.theme.StatusDegraded
import com.f4f.arena.ui.theme.StatusError
import com.f4f.arena.ui.theme.StatusHealthy
import kotlinx.coroutines.launch

sealed interface HealthUiState {
    object Idle : HealthUiState
    object Loading : HealthUiState
    data class Success(val response: HealthResponse) : HealthUiState
    data class Error(val message: String) : HealthUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen() {
    val coroutineScope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf<HealthUiState>(HealthUiState.Idle) }

    fun checkHealth() {
        coroutineScope.launch {
            uiState = HealthUiState.Loading
            try {
                val response = NetworkClient.healthApi.getHealth()
                if (response.isSuccessful && response.body() != null) {
                    uiState = HealthUiState.Success(response.body()!!)
                } else {
                    val code = response.code()
                    uiState = HealthUiState.Error("Server responded with HTTP $code")
                }
            } catch (e: Exception) {
                uiState = HealthUiState.Error(e.localizedMessage ?: "Network connection failed")
            }
        }
    }

    LaunchedEffect(Unit) {
        checkHealth()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("F4F Arena", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = "System Connectivity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Configured Backend URL:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = NetworkClient.baseUrl,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            when (val state = uiState) {
                is HealthUiState.Idle, is HealthUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Text("Connecting to Go Backend...", style = MaterialTheme.typography.bodyLarge)
                }
                is HealthUiState.Success -> {
                    val isDbConnected = state.response.database == "connected"
                    val statusColor = if (isDbConnected) StatusHealthy else StatusDegraded

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isDbConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isDbConnected) "Go Backend Online" else "Degraded: DB Disconnected",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                            Divider()
                            Text("Service: ${state.response.service}")
                            Text("Version: ${state.response.version}")
                            Text("Database Status: ${state.response.database}")
                            Text("Timestamp: ${state.response.timestamp}")
                        }
                    }
                }
                is HealthUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = StatusError,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Connection Failed",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusError
                                )
                            }
                            Text(text = state.message, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Button(
                onClick = { checkHealth() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recheck Status")
            }
        }
    }
}
