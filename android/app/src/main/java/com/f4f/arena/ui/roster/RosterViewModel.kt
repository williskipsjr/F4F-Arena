package com.f4f.arena.ui.roster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f4f.arena.data.api.NetworkClient
import com.f4f.arena.data.model.CreatePlayerRequest
import com.f4f.arena.data.model.Player
import com.f4f.arena.data.model.Position
import com.f4f.arena.data.model.UpdatePlayerRequest
import com.f4f.arena.data.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RosterUiState {
    object Loading : RosterUiState
    data class Success(
        val allPlayers: List<Player>,
        val filteredPlayers: List<Player>,
        val searchQuery: String = "",
        val selectedFilter: Position? = null,
        val keeperCount: Int = 0,
        val defenderCount: Int = 0,
        val midfielderCount: Int = 0,
        val strikerCount: Int = 0,
        val totalCount: Int = 0,
        val actionMessage: String? = null
    ) : RosterUiState
    data class Error(val message: String) : RosterUiState
}

class RosterViewModel(
    private val repository: PlayerRepository = PlayerRepository(NetworkClient.playerApi)
) : ViewModel() {

    private val _uiState = MutableStateFlow<RosterUiState>(RosterUiState.Loading)
    val uiState: StateFlow<RosterUiState> = _uiState.asStateFlow()

    private var rawPlayers: List<Player> = emptyList()
    private var currentQuery: String = ""
    private var currentFilter: Position? = null

    init {
        loadRoster()
    }

    fun loadRoster() {
        viewModelScope.launch {
            _uiState.value = RosterUiState.Loading
            repository.getPlayers()
                .onSuccess { players ->
                    rawPlayers = players
                    applyFilterAndSearch()
                }
                .onFailure { error ->
                    _uiState.value = RosterUiState.Error(error.localizedMessage ?: "Failed to load players")
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        currentQuery = query
        applyFilterAndSearch()
    }

    fun onFilterSelected(position: Position?) {
        currentFilter = if (currentFilter == position) null else position
        applyFilterAndSearch()
    }

    private fun applyFilterAndSearch(actionMessage: String? = null) {
        val filtered = rawPlayers.filter { player ->
            val matchesFilter = currentFilter == null || player.primaryPosition == currentFilter
            val matchesQuery = currentQuery.isBlank() || player.displayName.contains(currentQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }

        val gk = rawPlayers.count { it.primaryPosition == Position.KEEPER }
        val def = rawPlayers.count { it.primaryPosition == Position.DEFENDER }
        val mid = rawPlayers.count { it.primaryPosition == Position.MIDFIELDER }
        val str = rawPlayers.count { it.primaryPosition == Position.STRIKER }

        _uiState.value = RosterUiState.Success(
            allPlayers = rawPlayers,
            filteredPlayers = filtered,
            searchQuery = currentQuery,
            selectedFilter = currentFilter,
            keeperCount = gk,
            defenderCount = def,
            midfielderCount = mid,
            strikerCount = str,
            totalCount = rawPlayers.size,
            actionMessage = actionMessage
        )
    }

    fun clearActionMessage() {
        val current = _uiState.value as? RosterUiState.Success ?: return
        _uiState.value = current.copy(actionMessage = null)
    }

    fun togglePlayerActive(player: Player, token: String = com.f4f.arena.data.auth.SessionManager.getBearerToken()) {
        viewModelScope.launch {
            val updatedStatus = !player.isActive
            repository.updatePlayer(token, player.id, UpdatePlayerRequest(isActive = updatedStatus))
                .onSuccess { updated ->
                    rawPlayers = rawPlayers.map { if (it.id == updated.id) updated else it }
                    applyFilterAndSearch()
                }
                .onFailure { err ->
                    applyFilterAndSearch(actionMessage = err.localizedMessage ?: "Failed to update player")
                }
        }
    }

    fun createPlayer(name: String, position: Position, notes: String?, token: String = com.f4f.arena.data.auth.SessionManager.getBearerToken()) {
        viewModelScope.launch {
            repository.createPlayer(
                token,
                CreatePlayerRequest(
                    displayName = name,
                    primaryPosition = position.name,
                    isActive = true,
                    notes = notes
                )
            ).onSuccess {
                loadRoster()
            }.onFailure { err ->
                applyFilterAndSearch(actionMessage = err.localizedMessage ?: "Failed to add player")
            }
        }
    }
}
