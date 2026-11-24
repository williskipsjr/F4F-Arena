package com.f4f.arena.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f4f.arena.data.api.NetworkClient
import com.f4f.arena.data.model.*
import com.f4f.arena.data.repository.MatchRepository
import com.f4f.arena.data.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AttendanceUiState {
    object Loading : AttendanceUiState
    data class Success(
        val match: Match,
        val allPlayers: List<Player>,
        val filteredPlayers: List<Player>,
        val selectedPlayerIds: Set<String>,
        val searchQuery: String = "",
        val selectedFilter: Position? = null,
        val totalSelected: Int = 0,
        val keepersSelected: Int = 0,
        val defendersSelected: Int = 0,
        val midfieldersSelected: Int = 0,
        val strikersSelected: Int = 0,
        val suggestedTeams: Int = 0,
        val substitutesCount: Int = 0,
        val keeperDeficit: Int = 0,
        val isSaving: Boolean = false,
        val saveMessage: String? = null
    ) : AttendanceUiState
    data class Error(val message: String) : AttendanceUiState
}

class AttendanceViewModel(
    private val matchRepository: MatchRepository = MatchRepository(NetworkClient.matchApi),
    private val playerRepository: PlayerRepository = PlayerRepository(NetworkClient.playerApi)
) : ViewModel() {

    private val _uiState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Loading)
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private var currentMatch: Match? = null
    private var allRoster: List<Player> = emptyList()
    private val selectedIds = mutableSetOf<String>()
    private var currentQuery: String = ""
    private var currentFilter: Position? = null

    fun loadMatchAttendance(matchId: String) {
        viewModelScope.launch {
            _uiState.value = AttendanceUiState.Loading
            val matchRes = matchRepository.getMatch(matchId)
            val rosterRes = playerRepository.getPlayers()
            val attendeesRes = matchRepository.getAttendees(matchId)

            if (matchRes.isSuccess && rosterRes.isSuccess) {
                currentMatch = matchRes.getOrNull()!!
                allRoster = rosterRes.getOrNull()!!

                selectedIds.clear()
                attendeesRes.getOrNull()?.attendees?.forEach {
                    selectedIds.add(it.playerId)
                }

                updateCalculatedState()
            } else {
                _uiState.value = AttendanceUiState.Error("Failed to load match attendance")
            }
        }
    }

    fun togglePlayer(playerId: String) {
        if (selectedIds.contains(playerId)) {
            selectedIds.remove(playerId)
        } else {
            selectedIds.add(playerId)
        }
        updateCalculatedState()
    }

    fun selectAllActive() {
        allRoster.filter { it.isActive }.forEach {
            selectedIds.add(it.id)
        }
        updateCalculatedState()
    }

    fun clearSelection() {
        selectedIds.clear()
        updateCalculatedState()
    }

    fun onSearchQueryChanged(q: String) {
        currentQuery = q
        updateCalculatedState()
    }

    fun onFilterSelected(pos: Position?) {
        currentFilter = if (currentFilter == pos) null else pos
        updateCalculatedState()
    }

    private fun updateCalculatedState(isSaving: Boolean = false, saveMessage: String? = null) {
        val match = currentMatch ?: return

        val filtered = allRoster.filter { player ->
            val matchesFilter = currentFilter == null || player.primaryPosition == currentFilter
            val matchesQuery = currentQuery.isBlank() || player.displayName.contains(currentQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }

        val selectedPlayers = allRoster.filter { selectedIds.contains(it.id) }
        val total = selectedPlayers.size
        val gk = selectedPlayers.count { it.primaryPosition == Position.KEEPER }
        val def = selectedPlayers.count { it.primaryPosition == Position.DEFENDER }
        val mid = selectedPlayers.count { it.primaryPosition == Position.MIDFIELDER }
        val str = selectedPlayers.count { it.primaryPosition == Position.STRIKER }

        val teamSize = match.format.playersPerTeam
        val suggestedTeams = if (total >= teamSize) total / teamSize else 0
        val subs = if (total >= teamSize) total % teamSize else total
        val keeperDeficit = if (suggestedTeams > gk) suggestedTeams - gk else 0

        _uiState.value = AttendanceUiState.Success(
            match = match,
            allPlayers = allRoster,
            filteredPlayers = filtered,
            selectedPlayerIds = selectedIds.toSet(),
            searchQuery = currentQuery,
            selectedFilter = currentFilter,
            totalSelected = total,
            keepersSelected = gk,
            defendersSelected = def,
            midfieldersSelected = mid,
            strikersSelected = str,
            suggestedTeams = suggestedTeams,
            substitutesCount = subs,
            keeperDeficit = keeperDeficit,
            isSaving = isSaving,
            saveMessage = saveMessage
        )
    }

    fun saveAttendance(token: String = "dev-token", onSaved: (() -> Unit)? = null) {
        val match = currentMatch ?: return
        viewModelScope.launch {
            updateCalculatedState(isSaving = true)
            matchRepository.syncAttendees(token, match.id, selectedIds.toList())
                .onSuccess {
                    updateCalculatedState(isSaving = false, saveMessage = "Attendance saved successfully!")
                    onSaved?.invoke()
                }
                .onFailure { err ->
                    updateCalculatedState(isSaving = false, saveMessage = "Save failed: ${err.message}")
                }
        }
    }

    fun quickAddGuest(name: String, position: Position, token: String = "dev-token") {
        viewModelScope.launch {
            playerRepository.createPlayer(
                token,
                CreatePlayerRequest(displayName = name, primaryPosition = position.name, isActive = true)
            ).onSuccess { newPlayer ->
                allRoster = allRoster + newPlayer
                selectedIds.add(newPlayer.id)
                updateCalculatedState(saveMessage = "Guest added & checked in!")
            }
        }
    }
}
