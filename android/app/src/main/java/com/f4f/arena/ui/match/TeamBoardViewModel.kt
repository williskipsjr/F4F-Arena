package com.f4f.arena.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f4f.arena.data.api.NetworkClient
import com.f4f.arena.data.model.MatchStatus
import com.f4f.arena.data.model.MatchTeamsData
import com.f4f.arena.data.model.SubstitutePolicy
import com.f4f.arena.data.model.TeamPlayerDetail
import com.f4f.arena.data.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TeamBoardUiState {
    object Loading : TeamBoardUiState
    data class Success(
        val data: MatchTeamsData,
        val isGenerating: Boolean = false,
        val isMutating: Boolean = false,
        val selectedPlayerForSwap: TeamPlayerDetail? = null,
        val bannerMessage: String? = null
    ) : TeamBoardUiState
    data class Error(val message: String) : TeamBoardUiState
}

class TeamBoardViewModel(
    private val repository: TeamRepository = TeamRepository(NetworkClient.teamApi)
) : ViewModel() {

    private val _uiState = MutableStateFlow<TeamBoardUiState>(TeamBoardUiState.Loading)
    val uiState: StateFlow<TeamBoardUiState> = _uiState.asStateFlow()

    private var currentMatchId: String? = null

    fun loadOrGenerateTeams(matchId: String, token: String = com.f4f.arena.data.auth.SessionManager.getBearerToken()) {
        currentMatchId = matchId
        viewModelScope.launch {
            _uiState.value = TeamBoardUiState.Loading
            val getResult = repository.getTeams(matchId)
            if (getResult.isSuccess) {
                val existing = getResult.getOrNull()
                if (existing != null && existing.teams.orEmpty().isNotEmpty()) {
                    _uiState.value = TeamBoardUiState.Success(data = existing)
                } else {
                    generateTeams(matchId, subPolicy = null, autoFormat = true, token = token)
                }
            } else {
                _uiState.value = TeamBoardUiState.Error(
                    getResult.exceptionOrNull()?.message ?: "Failed to load teams"
                )
            }
        }
    }

    fun generateTeams(
        matchId: String,
        subPolicy: SubstitutePolicy? = null,
        format: com.f4f.arena.data.model.MatchFormat? = null,
        autoFormat: Boolean = false,
        token: String = com.f4f.arena.data.auth.SessionManager.getBearerToken()
    ) {
        viewModelScope.launch {
            val prevSuccess = (_uiState.value as? TeamBoardUiState.Success)
            if (prevSuccess != null) {
                _uiState.value = prevSuccess.copy(isGenerating = true, selectedPlayerForSwap = null)
            } else {
                _uiState.value = TeamBoardUiState.Loading
            }

            val result = repository.generateTeams(
                token = token,
                matchId = matchId,
                subPolicy = subPolicy?.name,
                format = format?.name,
                autoFormat = autoFormat
            )
            result.onSuccess { data ->
                _uiState.value = TeamBoardUiState.Success(
                    data = data,
                    isGenerating = false,
                    selectedPlayerForSwap = null,
                    bannerMessage = "Teams generated successfully!"
                )
            }.onFailure { err ->
                if (prevSuccess != null) {
                    _uiState.value = prevSuccess.copy(
                        isGenerating = false,
                        bannerMessage = "Generation failed: ${err.message}"
                    )
                } else {
                    _uiState.value = TeamBoardUiState.Error(err.message ?: "Failed to generate teams")
                }
            }
        }
    }

    fun onPlayerClicked(player: TeamPlayerDetail, token: String = com.f4f.arena.data.auth.SessionManager.getBearerToken()) {
        val state = (_uiState.value as? TeamBoardUiState.Success) ?: return
        val matchId = currentMatchId ?: return

        // If match is finalised, editing is locked
        if (state.data.status == MatchStatus.FINALISED) {
            _uiState.value = state.copy(bannerMessage = "Match is finalised. Re-open to edit.")
            return
        }

        val currentSelected = state.selectedPlayerForSwap
        if (currentSelected == null) {
            // Step 1: Select candidate for swap
            _uiState.value = state.copy(
                selectedPlayerForSwap = player,
                bannerMessage = "Selected ${player.displayName}. Tap another player or substitute to swap."
            )
        } else if (currentSelected.id == player.id) {
            // Deselect
            _uiState.value = state.copy(
                selectedPlayerForSwap = null,
                bannerMessage = null
            )
        } else {
            // Step 2: Second player tapped! Execute swap
            executeSwap(matchId, currentSelected.id, player.id, token)
        }
    }

    fun clearSwapSelection() {
        val state = (_uiState.value as? TeamBoardUiState.Success) ?: return
        _uiState.value = state.copy(selectedPlayerForSwap = null, bannerMessage = null)
    }

    private fun executeSwap(matchId: String, playerAId: String, playerBId: String, token: String) {
        viewModelScope.launch {
            val state = (_uiState.value as? TeamBoardUiState.Success) ?: return@launch
            _uiState.value = state.copy(isMutating = true, selectedPlayerForSwap = null)

            val result = repository.swapPlayers(token, matchId, playerAId, playerBId)
            result.onSuccess { updatedData ->
                _uiState.value = TeamBoardUiState.Success(
                    data = updatedData,
                    isMutating = false,
                    selectedPlayerForSwap = null,
                    bannerMessage = "Players swapped successfully! [Manual Override]"
                )
            }.onFailure { err ->
                _uiState.value = state.copy(
                    isMutating = false,
                    selectedPlayerForSwap = null,
                    bannerMessage = "Swap failed: ${err.message}"
                )
            }
        }
    }

    fun movePlayer(
        playerId: String,
        targetTeamId: String?,
        isSubstitute: Boolean,
        token: String = com.f4f.arena.data.auth.SessionManager.getBearerToken()
    ) {
        val matchId = currentMatchId ?: return
        viewModelScope.launch {
            val state = (_uiState.value as? TeamBoardUiState.Success) ?: return@launch
            _uiState.value = state.copy(isMutating = true, selectedPlayerForSwap = null)

            val result = repository.movePlayer(token, matchId, playerId, targetTeamId, isSubstitute)
            result.onSuccess { updatedData ->
                _uiState.value = TeamBoardUiState.Success(
                    data = updatedData,
                    isMutating = false,
                    selectedPlayerForSwap = null,
                    bannerMessage = "Player moved successfully! [Manual Override]"
                )
            }.onFailure { err ->
                _uiState.value = state.copy(
                    isMutating = false,
                    bannerMessage = "Move failed: ${err.message}"
                )
            }
        }
    }

    fun finaliseMatch(token: String = com.f4f.arena.data.auth.SessionManager.getBearerToken()) {
        val matchId = currentMatchId ?: return
        viewModelScope.launch {
            val state = (_uiState.value as? TeamBoardUiState.Success) ?: return@launch
            _uiState.value = state.copy(isMutating = true)

            val result = repository.finaliseMatch(token, matchId)
            result.onSuccess { updatedData ->
                _uiState.value = TeamBoardUiState.Success(
                    data = updatedData,
                    isMutating = false,
                    selectedPlayerForSwap = null,
                    bannerMessage = "Match teams finalised! Ready for WhatsApp export ⚽"
                )
            }.onFailure { err ->
                _uiState.value = state.copy(
                    isMutating = false,
                    bannerMessage = "Finalise failed: ${err.message}"
                )
            }
        }
    }
}
