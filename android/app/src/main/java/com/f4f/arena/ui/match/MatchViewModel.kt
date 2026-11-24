package com.f4f.arena.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f4f.arena.data.api.NetworkClient
import com.f4f.arena.data.model.CreateMatchRequest
import com.f4f.arena.data.model.Match
import com.f4f.arena.data.model.MatchFormat
import com.f4f.arena.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MatchUiState {
    object Loading : MatchUiState
    data class Success(
        val matches: List<Match>,
        val activeMatch: Match? = null
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
            repository.listMatches()
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
        token: String = "dev-token",
        onSuccess: (Match) -> Unit
    ) {
        viewModelScope.launch {
            val req = CreateMatchRequest(
                title = title.ifBlank { "Session ${format.label}" },
                format = format.name,
                venue = venue?.ifBlank { null }
            )
            repository.createMatch(token, req)
                .onSuccess { newMatch ->
                    loadMatches()
                    onSuccess(newMatch)
                }
                .onFailure {
                    loadMatches()
                }
        }
    }
}
