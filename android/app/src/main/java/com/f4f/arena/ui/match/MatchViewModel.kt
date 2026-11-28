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
                matchDate = computeKickoffIso(kickoffTime),
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

    fun loadMatchComments(
        matchId: String,
        onResult: (Result<List<com.f4f.arena.data.model.MatchComment>>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repository.getMatchComments(matchId))
        }
    }

    fun postMatchComment(
        matchId: String,
        message: String,
        token: String = SessionManager.getBearerToken(),
        onResult: (Result<com.f4f.arena.data.model.MatchComment>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repository.postMatchComment(token, matchId, message))
        }
    }

    fun deleteMatchComment(
        matchId: String,
        commentId: String,
        token: String = SessionManager.getBearerToken(),
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repository.deleteMatchComment(token, matchId, commentId))
        }
    }

    fun endMatch(
        matchId: String,
        token: String = SessionManager.getBearerToken()
    ) {
        viewModelScope.launch {
            repository.updateMatch(
                token,
                matchId,
                UpdateMatchRequest(status = "COMPLETED", isPollOpen = false)
            ).onSuccess { updatedMatch ->
                val current = _uiState.value as? MatchUiState.Success ?: return@onSuccess
                val updatedList = current.matches.map { if (it.id == updatedMatch.id) updatedMatch else it }
                _uiState.value = current.copy(
                    matches = updatedList,
                    actionMessage = "${updatedMatch.title} marked as Match Ended"
                )
            }.onFailure { err ->
                val current = _uiState.value as? MatchUiState.Success ?: return@onFailure
                _uiState.value = current.copy(actionMessage = err.localizedMessage ?: "Failed to end match")
            }
        }
    }

    fun deleteMatch(
        matchId: String,
        token: String = SessionManager.getBearerToken()
    ) {
        viewModelScope.launch {
            repository.deleteMatch(token, matchId)
                .onSuccess {
                    val current = _uiState.value as? MatchUiState.Success ?: return@onSuccess
                    _uiState.value = current.copy(
                        matches = current.matches.filterNot { it.id == matchId },
                        actionMessage = "Poll deleted"
                    )
                }
                .onFailure { err ->
                    val current = _uiState.value as? MatchUiState.Success ?: return@onFailure
                    _uiState.value = current.copy(actionMessage = err.localizedMessage ?: "Failed to delete poll")
                }
        }
    }

    private fun computeKickoffIso(kickoffTime: String?): String? {
        val raw = kickoffTime?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val patterns = listOf("h:mm a", "h:mma", "HH:mm", "H:mm")
        for (p in patterns) {
            try {
                val timeFmt = java.text.SimpleDateFormat(p, java.util.Locale.US)
                val parsed = timeFmt.parse(raw.uppercase(java.util.Locale.US)) ?: continue
                val timeCal = java.util.Calendar.getInstance().apply { time = parsed }
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY))
                    set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE))
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val isoFmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                return isoFmt.format(cal.time)
            } catch (_: Exception) {
            }
        }
        return null
    }
}
