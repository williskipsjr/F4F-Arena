package com.f4f.arena.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f4f.arena.data.api.NetworkClient
import com.f4f.arena.data.auth.SessionManager
import com.f4f.arena.data.model.CreateMatchRequest
import com.f4f.arena.data.model.Match
import com.f4f.arena.data.model.MatchFormat
import com.f4f.arena.data.model.MatchVoteDetail
import com.f4f.arena.data.model.UpdateMatchRequest
import com.f4f.arena.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MatchUiState {
    object Loading : MatchUiState
    data class Success(
        val matches: List<Match>,
        val activeMatch: Match? = null,
        val actionMessage: String? = null
    ) : MatchUiState
    data class Error(val message: String) : MatchUiState
}

class MatchViewModel(
    private val repository: MatchRepository = MatchRepository(NetworkClient.matchApi)
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchUiState>(MatchUiState.Loading)
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    init {
        loadMatches()
    }

    fun loadMatches() {
        viewModelScope.launch {
            _uiState.value = MatchUiState.Loading
            val token = SessionManager.getBearerToken().ifBlank { null }
            repository.listMatches(token = token)
                .onSuccess { matches ->
                    _uiState.value = MatchUiState.Success(
                        matches = matches,
                        activeMatch = matches.firstOrNull()
                    )
                }
                .onFailure { error ->
                    _uiState.value = MatchUiState.Error(error.localizedMessage ?: "Failed to load matches")
                }
        }
    }

    fun createMatch(
        title: String,
        format: MatchFormat,
        venue: String?,
        kickoffTime: String? = null,
        isPollOpen: Boolean = true,
        token: String = SessionManager.getBearerToken(),
        onSuccess: (Match) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val req = CreateMatchRequest(
                title = title.ifBlank { "Session ${format.label}" },
                kickoffTime = kickoffTime?.trim()?.ifBlank { null },
                format = format.name,
                venue = venue?.ifBlank { null },
                isPollOpen = isPollOpen
            )
            repository.createMatch(token, req)
                .onSuccess { newMatch ->
                    loadMatches()
                    onSuccess(newMatch)
                }
                .onFailure { err ->
                    onError(err.localizedMessage ?: "Failed to create match")
                }
        }
    }

    fun castVote(
        matchId: String,
        vote: String,
        token: String = SessionManager.getBearerToken()
    ) {
        viewModelScope.launch {
            repository.castVote(token, matchId, vote)
                .onSuccess { updatedMatch ->
                    val current = _uiState.value as? MatchUiState.Success ?: return@onSuccess
                    val updatedList = current.matches.map { if (it.id == updatedMatch.id) updatedMatch else it }
                    _uiState.value = current.copy(
                        matches = updatedList,
                        actionMessage = "Voted $vote for ${updatedMatch.title}!"
                    )
                }
                .onFailure { err ->
                    val current = _uiState.value as? MatchUiState.Success ?: return@onFailure
                    _uiState.value = current.copy(actionMessage = err.localizedMessage ?: "Vote failed")
                }
        }
    }

    fun updateMatchFormat(
        matchId: String,
        format: MatchFormat,
        token: String = SessionManager.getBearerToken(),
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateMatch(token, matchId, UpdateMatchRequest(format = format.name))
                .onSuccess { updatedMatch ->
                    val current = _uiState.value as? MatchUiState.Success ?: return@onSuccess
                    val updatedList = current.matches.map { if (it.id == updatedMatch.id) updatedMatch else it }
                    _uiState.value = current.copy(
                        matches = updatedList,
                        actionMessage = "Format set to ${format.label}"
                    )
                    onDone()
                }
                .onFailure { err ->
                    val current = _uiState.value as? MatchUiState.Success ?: return@onFailure
                    _uiState.value = current.copy(actionMessage = err.localizedMessage ?: "Failed to update format")
                }
        }
    }

    fun clearActionMessage() {
        val current = _uiState.value as? MatchUiState.Success ?: return
        _uiState.value = current.copy(actionMessage = null)
    }

    fun loadMatchVotes(
        matchId: String,
        onResult: (Result<List<MatchVoteDetail>>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.getMatchVotes(matchId)
            onResult(res)
        }
    }
}
