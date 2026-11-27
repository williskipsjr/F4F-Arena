package com.f4f.arena

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.f4f.arena.data.api.NetworkClient
import com.f4f.arena.data.auth.SessionManager
import com.f4f.arena.data.model.AppVersionResponse
import com.f4f.arena.data.model.CreateFeedbackRequest
import com.f4f.arena.data.model.MatchFormat as ApiMatchFormat
import com.f4f.arena.data.model.MatchStatus
import com.f4f.arena.data.model.MatchVoteDetail
import com.f4f.arena.data.model.Player
import com.f4f.arena.data.model.Position
import com.f4f.arena.data.model.SubstitutePolicy
import com.f4f.arena.ui.components.F4FBottomTab
import com.f4f.arena.ui.components.FloatingDarkBottomBar
import com.f4f.arena.ui.components.FootballLoadingAnimation
import com.f4f.arena.ui.match.*
import com.f4f.arena.ui.roster.AddPlayerDialog
import com.f4f.arena.ui.roster.RosterUiState
import com.f4f.arena.ui.roster.RosterViewModel
import com.f4f.arena.ui.screens.*
import com.f4f.arena.ui.state.*
import com.f4f.arena.ui.theme.F4FMaterialTheme
import com.f4f.arena.util.WhatsAppFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            F4FMaterialTheme {
                F4FArenaApp()
            }
        }
    }
}

@Composable
private fun F4FArenaApp(
    matchViewModel: MatchViewModel = viewModel(),
    rosterViewModel: RosterViewModel = viewModel(),
    attendanceViewModel: AttendanceViewModel = viewModel(),
    teamBoardViewModel: TeamBoardViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(F4FBottomTab.HOME) }
    var searchQuery by remember { mutableStateOf("") }
    var activeAttendanceMatchId by remember { mutableStateOf<String?>(null) }
    var activeTeamBoardMatchId by remember { mutableStateOf<String?>(null) }

    var showAuthSheet by remember { mutableStateOf(false) }
    var authInitialSignUp by remember { mutableStateOf(false) }
    var skippedOnboarding by remember { mutableStateOf(false) }
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var availableUpdate by remember { mutableStateOf<AppVersionResponse?>(null) }

    var feedbackItems by remember { mutableStateOf<List<FeedbackItemUi>>(emptyList()) }
    var isSubmittingFeedback by remember { mutableStateOf(false) }

    var showCreatePollModal by remember { mutableStateOf(false) }
    var createPollLoading by remember { mutableStateOf(false) }
    var createPollError by remember { mutableStateOf<String?>(null) }

    var votersMatch by remember { mutableStateOf<MatchCardUi?>(null) }
    var votersLoading by remember { mutableStateOf(false) }
    var votersList by remember { mutableStateOf<List<PollVoteUi>>(emptyList()) }
    var commentsList by remember { mutableStateOf<List<PollCommentUi>>(emptyList()) }
    var postingComment by remember { mutableStateOf(false) }

    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }
    var showQuickGuestDialog by remember { mutableStateOf(false) }
    var showWhatsAppDialog by remember { mutableStateOf(false) }

    var healthUi by remember {
        mutableStateOf(
            SystemHealthUi(
                baseUrl = NetworkClient.baseUrl,
                apiConnected = false,
                postgresConnected = false,
                latencyMs = 0,
                lastChecked = "Checking..."
            )
        )
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    fun checkSystemHealth() {
        scope.launch {
            val start = System.currentTimeMillis()
            try {
                val res = NetworkClient.healthApi.getHealth()
                val elapsed = (System.currentTimeMillis() - start).toInt()
                val now = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    healthUi = SystemHealthUi(
                        baseUrl = NetworkClient.baseUrl,
                        apiConnected = true,
                        postgresConnected = body.database == "connected",
                        latencyMs = elapsed,
                        lastChecked = now
                    )
                } else {
                    healthUi = healthUi.copy(apiConnected = false, postgresConnected = false, lastChecked = now)
                }
            } catch (_: Exception) {
                val now = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
                healthUi = healthUi.copy(apiConnected = false, postgresConnected = false, lastChecked = now)
            }
        }
    }

    LaunchedEffect(Unit) {
        checkSystemHealth()
        try {
            val res = NetworkClient.healthApi.getAppVersion()
            if (res.isSuccessful && res.body() != null) {
                val info = res.body()!!
                if (info.versionCode > BuildConfig.VERSION_CODE && info.downloadUrl.isNotBlank()) {
                    availableUpdate = info
                }
            }
        } catch (_: Exception) {
        }
    }

    val token by SessionManager.token.collectAsState()
    val displayName by SessionManager.displayName.collectAsState()
    val username by SessionManager.adminUsername.collectAsState()
    val role by SessionManager.role.collectAsState()
    val playerId by SessionManager.playerId.collectAsState()
    val authPromptRequired by SessionManager.authPromptRequired.collectAsState()

    val isLoggedIn = !token.isNullOrBlank()
    val sessionUi = if (isLoggedIn) {
        UserSessionUi(
            id = playerId ?: "admin",
            name = displayName ?: username ?: "Member",
            username = username ?: "member",
            role = if (role == "ADMIN") UserRole.ADMIN else UserRole.PLAYER,
            playerId = playerId
        )
    } else null
    val isAdmin = sessionUi?.role == UserRole.ADMIN

    fun loadFeedbackIfAdmin() {
        if (!isAdmin) return
        scope.launch {
            try {
                val res = NetworkClient.feedbackApi.listFeedback(SessionManager.getBearerToken())
                if (res.isSuccessful && res.body() != null) {
                    feedbackItems = res.body()!!.data.map {
                        FeedbackItemUi(
                            id = it.id,
                            authorName = it.authorName,
                            category = it.category,
                            message = it.message,
                            createdAtLabel = formatIsoTimestamp(it.createdAt)
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(isAdmin) {
        if (isAdmin) loadFeedbackIfAdmin()
    }

    LaunchedEffect(authPromptRequired) {
        if (authPromptRequired) {
            showAuthSheet = true
            SessionManager.consumeAuthPrompt()
        }
    }

    LaunchedEffect(activeAttendanceMatchId) {
        if (activeAttendanceMatchId != null) {
            attendanceViewModel.loadMatchAttendance(activeAttendanceMatchId!!)
        }
    }

    LaunchedEffect(activeTeamBoardMatchId) {
        if (activeTeamBoardMatchId != null) {
            teamBoardViewModel.loadOrGenerateTeams(activeTeamBoardMatchId!!)
        }
    }

    val matchState by matchViewModel.uiState.collectAsState()
    val rosterState by rosterViewModel.uiState.collectAsState()
    val attendanceState by attendanceViewModel.uiState.collectAsState()
    val teamBoardState by teamBoardViewModel.uiState.collectAsState()

    val matchActionMsg = (matchState as? MatchUiState.Success)?.actionMessage
    LaunchedEffect(matchActionMsg) {
        if (matchActionMsg != null) {
            snackbarHostState.showSnackbar(matchActionMsg)
            matchViewModel.clearActionMessage()
        }
    }

    val rosterActionMsg = (rosterState as? RosterUiState.Success)?.actionMessage
    LaunchedEffect(rosterActionMsg) {
        if (rosterActionMsg != null) {
            snackbarHostState.showSnackbar(rosterActionMsg)
            rosterViewModel.clearActionMessage()
        }
    }

    val attendanceSaveMsg = (attendanceState as? AttendanceUiState.Success)?.saveMessage
    LaunchedEffect(attendanceSaveMsg) {
        if (attendanceSaveMsg != null) {
            snackbarHostState.showSnackbar(attendanceSaveMsg)
            attendanceViewModel.clearSaveMessage()
        }
    }

    val teamBannerMsg = (teamBoardState as? TeamBoardUiState.Success)?.bannerMessage
    LaunchedEffect(teamBannerMsg) {
        if (teamBannerMsg != null) {
            snackbarHostState.showSnackbar(teamBannerMsg)
        }
    }

    BackHandler(enabled = activeTeamBoardMatchId != null || activeAttendanceMatchId != null) {
        when {
            activeTeamBoardMatchId != null -> activeTeamBoardMatchId = null
            activeAttendanceMatchId != null -> activeAttendanceMatchId = null
        }
    }

    fun openVotersForMatch(card: MatchCardUi) {
        votersMatch = card
        votersLoading = true
        votersList = emptyList()
        commentsList = emptyList()
        matchViewModel.loadMatchVotes(card.id) { res ->
            votersLoading = false
            res.onSuccess { list -> votersList = list.map { it.toUi() } }
                .onFailure { err ->
                    scope.launch { snackbarHostState.showSnackbar(err.localizedMessage ?: "Failed to load votes") }
                }
        }
        matchViewModel.loadMatchComments(card.id) { res ->
            res.onSuccess { list -> commentsList = list.map { it.toUi() } }
        }
    }

    val allMatchCards: List<MatchCardUi> = when (val st = matchState) {
        is MatchUiState.Success -> st.matches.map { it.toCardUi() }
        else -> emptyList()
    }
    val filteredMatchCards = allMatchCards.filter {
        searchQuery.isBlank() ||
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.venue.contains(searchQuery, ignoreCase = true)
    }
    val featuredPoll = filteredMatchCards.firstOrNull { it.pollOpen } ?: filteredMatchCards.firstOrNull()
    val exploreMatches = if (featuredPoll != null) filteredMatchCards.filter { it.id != featuredPoll.id } else filteredMatchCards

    val rawPlayers: List<Player> = (rosterState as? RosterUiState.Success)?.allPlayers ?: emptyList()
    val uiPlayers: List<PlayerUi> = rawPlayers.map { it.toUi() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !isLoggedIn && !skippedOnboarding -> {
                OnboardingWelcomeScreen(
                    onSignUpClick = {
                        authInitialSignUp = true
                        showAuthSheet = true
                    },
                    onSignInClick = {
                        authInitialSignUp = false
                        showAuthSheet = true
                    },
                    onBrowseAsGuest = { skippedOnboarding = true }
                )
            }

            activeTeamBoardMatchId != null -> {
                val tbSuccess = teamBoardState as? TeamBoardUiState.Success
                if (tbSuccess != null) {
                    val data = tbSuccess.data
                    TeamBoardScreen(
                        format = data.format.toUi(),
                        policy = data.subPolicy.toUi(),
                        teams = data.teams.map { t ->
                            GeneratedTeamUi(
                                id = t.id,
                                name = t.teamName,
                                kitColorName = t.bibColor ?: "Bibs",
                                kitColorHex = bibHex(t.bibColor),
                                players = t.players.map { p ->
                                    TeamSlotUi(
                                        playerId = p.id,
                                        name = p.displayName,
                                        position = p.assignedPosition.toUi(),
                                        isManual = p.isManualOverride
                                    )
                                },
                                needsEmergencyGk = t.players.none { it.assignedPosition == Position.KEEPER }
                            )
                        },
                        bench = data.substitutes.map { s ->
                            TeamSlotUi(
                                playerId = s.id,
                                name = s.displayName,
                                position = s.primaryPosition.toUi(),
                                isManual = s.isManualOverride
                            )
                        },
                        isAdmin = isAdmin,
                        isFinalised = data.status == MatchStatus.FINALISED,
                        isGenerating = tbSuccess.isGenerating,
                        swapSourcePlayerId = tbSuccess.selectedPlayerForSwap?.id,
                        onBack = { activeTeamBoardMatchId = null },
                        onPolicyChange = { newPolicy ->
                            teamBoardViewModel.generateTeams(activeTeamBoardMatchId!!, newPolicy.toApi())
                        },
                        onFormatChange = { newFormat ->
                            teamBoardViewModel.generateTeams(
                                matchId = activeTeamBoardMatchId!!,
                                format = newFormat.toApi(),
                                autoFormat = false
                            )
                        },
                        onAutoFormat = {
                            teamBoardViewModel.generateTeams(
                                matchId = activeTeamBoardMatchId!!,
                                autoFormat = true
                            )
                        },
                        onReshuffle = {
                            teamBoardViewModel.generateTeams(activeTeamBoardMatchId!!)
                        },
                        onTapPlayerForSwap = { pid ->
                            val target = (data.teams.flatMap { it.players } + data.substitutes).firstOrNull { it.id == pid }
                            if (target != null) teamBoardViewModel.onPlayerClicked(target)
                        },
                        onMovePlayer = { pid, targetTeamId ->
                            teamBoardViewModel.movePlayer(pid, targetTeamId, isSubstitute = targetTeamId == null)
                        },
                        onToggleFinalise = {
                            teamBoardViewModel.finaliseMatch()
                        },
                        onOpenExport = { showWhatsAppDialog = true }
                    )

                    if (showWhatsAppDialog) {
                        val formatted = remember(data) { WhatsAppFormatter.format(data) }
                        WhatsAppExportDialog(
                            formattedText = formatted,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(formatted))
                                showWhatsAppDialog = false
                                scope.launch { snackbarHostState.showSnackbar("Copied to clipboard for WhatsApp!") }
                            },
                            onShare = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, formatted)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Teams via"))
                            },
                            onDismiss = { showWhatsAppDialog = false }
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (teamBoardState is TeamBoardUiState.Error) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text((teamBoardState as TeamBoardUiState.Error).message)
                                Button(onClick = { teamBoardViewModel.loadOrGenerateTeams(activeTeamBoardMatchId!!) }) {
                                    Text("Retry")
                                }
                                TextButton(onClick = { activeTeamBoardMatchId = null }) { Text("Back") }
                            }
                        } else {
                            FootballLoadingAnimation(message = "Building teams...")
                        }
                    }
                }
            }

            activeAttendanceMatchId != null -> {
                val attSuccess = attendanceState as? AttendanceUiState.Success
                if (attSuccess != null) {
                    AttendanceCheckInScreen(
                        format = attSuccess.match.format.toUi(),
                        players = attSuccess.allPlayers.map { it.toUi() },
                        checkedInIds = attSuccess.selectedPlayerIds,
                        isSaving = attSuccess.isSaving,
                        onBack = { activeAttendanceMatchId = null },
                        onTogglePlayer = { attendanceViewModel.togglePlayer(it) },
                        onSelectAll = { attendanceViewModel.selectAllActive() },
                        onClearAll = { attendanceViewModel.clearSelection() },
                        onQuickAddGuest = { showQuickGuestDialog = true },
                        onSaveAttendance = { attendanceViewModel.saveAttendance() },
                        onGenerateTeams = {
                            attendanceViewModel.saveAttendance {
                                activeTeamBoardMatchId = activeAttendanceMatchId
                            }
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FootballLoadingAnimation(message = "Loading attendance...")
                    }
                }
            }

            else -> {
                when (selectedTab) {
                    F4FBottomTab.HOME -> {
                        if (matchState is MatchUiState.Loading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                FootballLoadingAnimation(message = "Loading pitch...")
                            }
                        } else {
                            HomeDashboardScreen(
                                session = sessionUi,
                                searchQuery = searchQuery,
                                featuredPoll = featuredPoll,
                                exploreMatches = exploreMatches,
                                onSearchChange = { searchQuery = it },
                                onAvatarClick = {
                                    if (isLoggedIn) selectedTab = F4FBottomTab.SYSTEM else showAuthSheet = true
                                },
                                onVote = { matchId, voteIn ->
                                    if (!isLoggedIn) {
                                        showAuthSheet = true
                                    } else {
                                        matchViewModel.castVote(matchId, if (voteIn) "IN" else "OUT")
                                    }
                                },
                                onOpenVoters = { matchId ->
                                    allMatchCards.firstOrNull { it.id == matchId }?.let { openVotersForMatch(it) }
                                },
                                onSwitchFormat = { matchId, fmt ->
                                    matchViewModel.updateMatchFormat(matchId, fmt.toApi())
                                },
                                onOpenTeams = { matchId -> activeTeamBoardMatchId = matchId },
                                onOpenCheckIn = { matchId -> activeAttendanceMatchId = matchId },
                                onEndMatch = { matchId -> matchViewModel.endMatch(matchId) },
                                onDeleteMatch = { matchId -> matchViewModel.deleteMatch(matchId) },
                                onSeeAllMatches = { selectedTab = F4FBottomTab.MATCHES },
                                onCreateNewPoll = { showCreatePollModal = true }
                            )
                        }
                    }

                    F4FBottomTab.MATCHES -> {
                        if (matchState is MatchUiState.Loading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                FootballLoadingAnimation(message = "Loading matches...")
                            }
                        } else {
                            AllMatchesScreen(
                                matches = allMatchCards,
                                isAdmin = isAdmin,
                                onBack = { selectedTab = F4FBottomTab.HOME },
                                onOpenVoters = { matchId ->
                                    allMatchCards.firstOrNull { it.id == matchId }?.let { openVotersForMatch(it) }
                                },
                                onOpenTeams = { matchId -> activeTeamBoardMatchId = matchId },
                                onOpenCheckIn = { matchId -> activeAttendanceMatchId = matchId },
                                onDeleteMatch = { matchId -> matchViewModel.deleteMatch(matchId) }
                            )
                        }
                    }

                    F4FBottomTab.ROSTER -> {
                        RosterScreen(
                            players = uiPlayers,
                            isAdmin = isAdmin,
                            onEditPlayer = { playerUi ->
                                editingPlayer = rawPlayers.firstOrNull { it.id == playerUi.id }
                            },
                            onToggleActive = { pid, _ ->
                                rawPlayers.firstOrNull { it.id == pid }?.let { rosterViewModel.togglePlayerActive(it) }
                            },
                            onAddPlayer = { showAddPlayerDialog = true }
                        )
                    }

                    F4FBottomTab.SYSTEM -> {
                        UpdatesAndFeedbackScreen(
                            health = healthUi,
                            session = sessionUi,
                            feedbackItems = feedbackItems,
                            isSubmittingFeedback = isSubmittingFeedback,
                            onSubmitFeedback = { category, message ->
                                scope.launch {
                                    isSubmittingFeedback = true
                                    try {
                                        val res = NetworkClient.feedbackApi.submitFeedback(
                                            SessionManager.getBearerToken(),
                                            CreateFeedbackRequest(category, message)
                                        )
                                        if (res.isSuccessful) {
                                            snackbarHostState.showSnackbar("Thanks! Feedback sent.")
                                            loadFeedbackIfAdmin()
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to send feedback (HTTP ${res.code()})")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to send feedback")
                                    } finally {
                                        isSubmittingFeedback = false
                                    }
                                }
                            },
                            onRecheck = { checkSystemHealth() },
                            onOpenAuth = {
                                authInitialSignUp = false
                                showAuthSheet = true
                            },
                            onSignOut = {
                                SessionManager.logout()
                                skippedOnboarding = false
                                matchViewModel.loadMatches()
                            },
                            onChangePassword = { currentPwd, newPwd, onResult ->
                                scope.launch {
                                    val res = SessionManager.changePassword(currentPwd, newPwd)
                                    onResult(res)
                                    res.onSuccess { msg ->
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            }
                        )
                    }
                }

                FloatingDarkBottomBar(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )
    }

    if (showAuthSheet) {
        AuthBottomSheet(
            initialSignUp = authInitialSignUp,
            isLoading = authLoading,
            errorMessage = authError,
            onDismiss = { if (!authLoading) showAuthSheet = false },
            onSignIn = { user, pass ->
                scope.launch {
                    authLoading = true
                    authError = null
                    val res = SessionManager.login(user, pass)
                    authLoading = false
                    if (res.isSuccess) {
                        showAuthSheet = false
                        matchViewModel.loadMatches()
                    } else {
                        authError = res.exceptionOrNull()?.message ?: "Sign in failed"
                    }
                }
            },
            onSignUp = { name, phone, age, pin, firebaseIdToken ->
                scope.launch {
                    authLoading = true
                    authError = null
                    val res = SessionManager.register(name, phone, age, pin, firebaseIdToken)
                    authLoading = false
                    if (res.isSuccess) {
                        showAuthSheet = false
                        matchViewModel.loadMatches()
                        rosterViewModel.loadRoster()
                    } else {
                        authError = res.exceptionOrNull()?.message ?: "Sign up failed"
                    }
                }
            },
            onResetPin = { user, phone, newPin ->
                scope.launch {
                    authLoading = true
                    authError = null
                    val res = SessionManager.resetPin(user, phone, newPin)
                    authLoading = false
                    if (res.isSuccess) {
                        authInitialSignUp = false
                        showAuthSheet = false
                        snackbarHostState.showSnackbar("PIN reset! Please sign in with your new PIN.")
                    } else {
                        authError = res.exceptionOrNull()?.message ?: "PIN reset failed"
                    }
                }
            }
        )
    }

    if (availableUpdate != null) {
        val upd = availableUpdate!!
        AlertDialog(
            onDismissRequest = { availableUpdate = null },
            title = { Text("Update Available (v${upd.versionName})") },
            text = {
                Text(
                    if (upd.releaseNotes.isNotBlank()) upd.releaseNotes
                    else "A newer version of F4F Arena is ready with the latest UI & fixes."
                )
            },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upd.downloadUrl))
                    context.startActivity(intent)
                    availableUpdate = null
                }) {
                    Text("Download Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { availableUpdate = null }) {
                    Text("Later")
                }
            }
        )
    }

    if (showCreatePollModal) {
        CreatePollModal(
            isCreating = createPollLoading,
            errorMessage = createPollError,
            onDismiss = { showCreatePollModal = false },
            onCreatePoll = { title, kickoff, venue, fmt, openPoll ->
                createPollLoading = true
                createPollError = null
                matchViewModel.createMatch(
                    title = title,
                    format = fmt.toApi(),
                    venue = venue,
                    kickoffTime = kickoff,
                    isPollOpen = openPoll,
                    onSuccess = {
                        createPollLoading = false
                        showCreatePollModal = false
                    },
                    onError = { err ->
                        createPollLoading = false
                        createPollError = err
                    }
                )
            }
        )
    }

    if (votersMatch != null) {
        val currentCard = votersMatch!!
        PollVotersSheet(
            matchTitle = currentCard.title,
            votes = votersList,
            comments = commentsList,
            isLoading = votersLoading,
            isLoggedIn = isLoggedIn,
            isAdmin = isAdmin,
            isPostingComment = postingComment,
            onRefresh = { openVotersForMatch(currentCard) },
            onPostComment = { msg ->
                postingComment = true
                matchViewModel.postMatchComment(currentCard.id, msg) { res ->
                    postingComment = false
                    res.onSuccess { created ->
                        commentsList = commentsList + created.toUi()
                    }.onFailure { err ->
                        scope.launch { snackbarHostState.showSnackbar(err.localizedMessage ?: "Failed to post comment") }
                    }
                }
            },
            onDeleteComment = { cid ->
                matchViewModel.deleteMatchComment(currentCard.id, cid) { res ->
                    res.onSuccess {
                        commentsList = commentsList.filterNot { it.id == cid }
                    }.onFailure { err ->
                        scope.launch { snackbarHostState.showSnackbar(err.localizedMessage ?: "Failed to delete comment") }
                    }
                }
            },
            onRequireSignIn = {
                votersMatch = null
                showAuthSheet = true
            },
            onDismiss = { votersMatch = null }
        )
    }

    if (showAddPlayerDialog) {
        AddPlayerDialog(
            onDismiss = { showAddPlayerDialog = false },
            onConfirm = { name, pos, notes ->
                rosterViewModel.createPlayer(name, pos, notes)
                showAddPlayerDialog = false
            }
        )
    }

    if (editingPlayer != null) {
        val target = editingPlayer!!
        AddPlayerDialog(
            initialName = target.displayName,
            initialPosition = target.primaryPosition,
            initialNotes = target.notes ?: "",
            dialogTitle = "Assign Position / Edit Player",
            confirmLabel = "Save",
            onDismiss = { editingPlayer = null },
            onConfirm = { name, pos, notes ->
                rosterViewModel.updatePlayerDetails(target.id, name, pos, notes)
                editingPlayer = null
            }
        )
    }

    if (showQuickGuestDialog) {
        AddPlayerDialog(
            dialogTitle = "Quick Add Guest",
            confirmLabel = "Add & Check In",
            onDismiss = { showQuickGuestDialog = false },
            onConfirm = { name, pos, _ ->
                attendanceViewModel.quickAddGuest(name, pos)
                showQuickGuestDialog = false
            }
        )
    }
}

private fun Position.toUi(): PlayerPosition = when (this) {
    Position.KEEPER -> PlayerPosition.GK
    Position.DEFENDER -> PlayerPosition.DEF
    Position.MIDFIELDER -> PlayerPosition.MID
    Position.STRIKER -> PlayerPosition.STR
}

private fun ApiMatchFormat.toUi(): MatchFormat = when (this) {
    ApiMatchFormat.FIVE_A_SIDE -> MatchFormat.FIVE_A_SIDE
    ApiMatchFormat.SIX_A_SIDE -> MatchFormat.SIX_A_SIDE
    ApiMatchFormat.SEVEN_A_SIDE -> MatchFormat.SEVEN_A_SIDE
    ApiMatchFormat.EIGHT_A_SIDE -> MatchFormat.EIGHT_A_SIDE
    ApiMatchFormat.NINE_A_SIDE -> MatchFormat.NINE_A_SIDE
    ApiMatchFormat.TEN_A_SIDE -> MatchFormat.TEN_A_SIDE
    ApiMatchFormat.ELEVEN_A_SIDE -> MatchFormat.ELEVEN_A_SIDE
}

private fun MatchFormat.toApi(): ApiMatchFormat = when (this) {
    MatchFormat.FIVE_A_SIDE -> ApiMatchFormat.FIVE_A_SIDE
    MatchFormat.SIX_A_SIDE -> ApiMatchFormat.SIX_A_SIDE
    MatchFormat.SEVEN_A_SIDE -> ApiMatchFormat.SEVEN_A_SIDE
    MatchFormat.EIGHT_A_SIDE -> ApiMatchFormat.EIGHT_A_SIDE
    MatchFormat.NINE_A_SIDE -> ApiMatchFormat.NINE_A_SIDE
    MatchFormat.TEN_A_SIDE -> ApiMatchFormat.TEN_A_SIDE
    MatchFormat.ELEVEN_A_SIDE -> ApiMatchFormat.ELEVEN_A_SIDE
}

private fun SubstitutePolicy.toUi(): TeamPolicy = when (this) {
    SubstitutePolicy.STRICT_SUBSTITUTES -> TeamPolicy.STRICT_SUBS
    SubstitutePolicy.DISTRIBUTE_ALL -> TeamPolicy.DISTRIBUTE_ALL
}

private fun TeamPolicy.toApi(): SubstitutePolicy = when (this) {
    TeamPolicy.STRICT_SUBS -> SubstitutePolicy.STRICT_SUBSTITUTES
    TeamPolicy.DISTRIBUTE_ALL -> SubstitutePolicy.DISTRIBUTE_ALL
}

private fun Player.toUi(): PlayerUi = PlayerUi(
    id = id,
    name = displayName,
    position = primaryPosition.toUi(),
    age = age,
    isActive = isActive,
    notes = notes ?: ""
)

private fun com.f4f.arena.data.model.MatchComment.toUi(): PollCommentUi = PollCommentUi(
    id = id,
    authorName = authorName,
    isAdmin = authorRole.equals("ADMIN", ignoreCase = true),
    message = message,
    createdAtLabel = formatIsoTimestamp(createdAt)
)

private fun MatchVoteDetail.toUi(): PollVoteUi = PollVoteUi(
    playerId = playerId,
    playerName = displayName,
    position = primaryPosition.toUi(),
    isIn = vote == "IN",
    votedAtLabel = formatIsoTimestamp(votedAt)
)

private fun com.f4f.arena.data.model.Match.toCardUi(): MatchCardUi {
    val timeline = computeTimeline(status, kickoffTime, matchDate, updatedAt, createdAt)
    return MatchCardUi(
        id = id,
        title = title,
        kickoffTime = kickoffTime ?: "TBD",
        dateLabel = formatMatchDate(matchDate ?: createdAt),
        timelineLabel = timeline.label,
        isOngoing = timeline.isOngoing,
        isEnded = timeline.isEnded,
        venue = venue ?: "Turf Ground",
        format = format.toUi(),
        pollOpen = isPollOpen && !timeline.isEnded,
        inCount = inCount,
        outCount = outCount,
        currentUserVoteIn = when (myVote) {
            "IN" -> true
            "OUT" -> false
            else -> null
        },
        teamsReady = status == MatchStatus.TEAMS_GENERATED || status == MatchStatus.FINALISED,
        finalised = status == MatchStatus.FINALISED
    )
}

private data class TimelineState(val label: String, val isOngoing: Boolean, val isEnded: Boolean)

private fun computeTimeline(
    status: MatchStatus,
    kickoffTime: String?,
    matchDate: String?,
    updatedAt: String?,
    createdAt: String?
): TimelineState {
    if (status == MatchStatus.COMPLETED) {
        val endedAt = parseIsoDate(matchDate ?: updatedAt ?: createdAt)
        val rel = endedAt?.let { relativePastTime(it) } ?: "recently"
        return TimelineState("Match ended $rel", isOngoing = false, isEnded = true)
    }
    if (status == MatchStatus.CANCELLED) {
        return TimelineState("Match cancelled", isOngoing = false, isEnded = true)
    }
    val kickoffDate = parseIsoDate(matchDate)
    if (kickoffDate != null) {
        val diffMs = System.currentTimeMillis() - kickoffDate.time
        val threeHoursMs = 3 * 60 * 60 * 1000L
        if (diffMs in 0..threeHoursMs) {
            val startedStr = kickoffTime?.let { " · Started at $it" } ?: ""
            return TimelineState("Ongoing$startedStr", isOngoing = true, isEnded = false)
        }
        if (diffMs > threeHoursMs) {
            return TimelineState("Match ended ${relativePastTime(kickoffDate)}", isOngoing = false, isEnded = true)
        }
    }
    if (!kickoffTime.isNullOrBlank()) {
        return TimelineState("Starting at $kickoffTime", isOngoing = false, isEnded = false)
    }
    return TimelineState(formatMatchDate(createdAt), isOngoing = false, isEnded = false)
}

private fun parseIsoDate(isoString: String?): Date? {
    if (isoString.isNullOrBlank()) return null
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )
    for (pattern in formats) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return parser.parse(isoString)
        } catch (_: Exception) {
        }
    }
    return null
}

private fun relativePastTime(date: Date): String {
    val diffMs = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
    val mins = diffMs / 60_000L
    val hours = mins / 60L
    val days = hours / 24L
    return when {
        days >= 2 -> "$days days ago"
        days == 1L -> "yesterday"
        hours >= 2 -> "$hours hours ago"
        hours == 1L -> "1 hour ago"
        mins >= 2 -> "$mins mins ago"
        else -> "just now"
    }
}

private fun bibHex(bibColor: String?): Long = when (bibColor?.lowercase(Locale.US)) {
    "orange" -> 0xFFF97316
    "green" -> 0xFF22C55E
    "blue" -> 0xFF3B82F6
    "yellow" -> 0xFFEAB308
    "red" -> 0xFFEF4444
    else -> 0xFF132A1E
}

private fun formatMatchDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "Today"
    return formatIsoTimestamp(iso, "EEE, MMM d")
}

private fun formatIsoTimestamp(isoString: String, outPattern: String = "MMM d, h:mm a"): String {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )
    for (pattern in formats) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(isoString) ?: continue
            return SimpleDateFormat(outPattern, Locale.getDefault()).format(date)
        } catch (_: Exception) {
        }
    }
    return isoString.take(16).replace("T", " ")
}
