package uz.angrykitten.spygame.ui.scoreboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.angrykitten.spygame.data.PreferencesRepository
import uz.angrykitten.spygame.data.SessionRepository
import uz.angrykitten.spygame.model.Player
import uz.angrykitten.spygame.network.NetworkRepository
import javax.inject.Inject

data class ScoreboardUiState(
    val players: List<Player> = emptyList(),
    val isHost: Boolean = false
)

@HiltViewModel
class ScoreboardViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val sessionRepository: SessionRepository,
    private val networkRepository: NetworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScoreboardUiState())
    val uiState: StateFlow<ScoreboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val myId = preferencesRepository.playerId.first()
            // Observe session changes live so the scoreboard reflects ongoing
            // score updates rather than a one-shot snapshot at construction.
            sessionRepository.gameState.collect { state ->
                val isHost = state?.room?.hostId == myId
                val players = state?.room?.players.orEmpty()
                val scores = state?.scores.orEmpty()

                val scoredPlayers = players.map { player ->
                    player.copy(score = scores[player.id] ?: player.score)
                }.sortedByDescending { it.score }

                _uiState.update {
                    it.copy(players = scoredPlayers, isHost = isHost)
                }
            }
        }
    }

    fun cleanup() {
        sessionRepository.clear()
        networkRepository.cleanup()
    }
}
