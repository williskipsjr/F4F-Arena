package com.f4f.arena.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f4f.arena.data.api.NetworkClient
import com.f4f.arena.data.model.MatchTeamsData
import com.f4f.arena.data.model.SubstitutePolicy
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

    fun loadOrGenerateTeams(matchId: String, token: String = "dev-token") {
        currentMatchId = matchId
        viewModelScope.launch {
            _uiState.value = TeamBoardUiState.Loading
            // Try fetching existing teams first
            val getResult = repository.getTeams(matchId)
            if (getResult.isSuccess && getResult.getOrNull()?.teams?.isNotEmpty() == true) {
                _uiState.value = TeamBoardUiState.Success(data = getResult.getOrNull()!!)
            } else {
                // Not generated yet, trigger generation
                generateTeams(matchId, subPolicy = null, token = token)
            }
        }
    }

    fun generateTeams(
        matchId: String,
        subPolicy: SubstitutePolicy? = null,
        token: String = "dev-token"
    ) {
        viewModelScope.launch {
            val prevSuccess = (_uiState.value as? TeamBoardUiState.Success)
            if (prevSuccess != null) {
                _uiState.value = prevSuccess.copy(isGenerating = true)
            } else {
                _uiState.value = TeamBoardUiState.Loading
            }

            val result = repository.generateTeams(token, matchId, subPolicy?.name)
            result.onSuccess { data ->
                _uiState.value = TeamBoardUiState.Success(
                    data = data,
                    isGenerating = false,
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
}
