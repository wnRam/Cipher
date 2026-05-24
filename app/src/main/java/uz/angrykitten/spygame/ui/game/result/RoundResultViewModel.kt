package uz.angrykitten.spygame.ui.game.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.angrykitten.spygame.data.PreferencesRepository
import uz.angrykitten.spygame.data.SessionRepository
import uz.angrykitten.spygame.model.*
import uz.angrykitten.spygame.network.NetworkRepository
import uz.angrykitten.spygame.sound.SoundEvent
import uz.angrykitten.spygame.sound.SoundManager
import javax.inject.Inject

data class RoundResultUiState(
    val winningSide: WinningSide = WinningSide.PLAYERS,
    val spyNames: List<String> = emptyList(),
    val location: String = "",
    val showSpyGuess: Boolean = false,
    val spyGuessOptions: List<String> = emptyList(),
    val spyGuessResult: Boolean? = null,
    val scores: Map<String, Int> = emptyMap(),
    val players: List<Player> = emptyList(),
    val isHost: Boolean = false,
    val myPlayerId: String = ""
)

@HiltViewModel
class RoundResultViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val networkRepository: NetworkRepository,
    private val sessionRepository: SessionRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoundResultUiState())
    val uiState: StateFlow<RoundResultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val myId = preferencesRepository.playerId.first()
            val state = sessionRepository.gameState.value
            val isHost = state?.room?.hostId == myId
            val players = state?.room?.players.orEmpty()
            val assignments = state?.assignments.orEmpty()
            val location = state?.currentLocation.orEmpty()
            val eliminatedPlayers = state?.eliminatedPlayers.orEmpty()

            // Determine winning side based on whether spies were eliminated
            val spyIds = assignments.filter { it.value.isSpy }.keys.toList()
            val spyCaught = spyIds.any { it in eliminatedPlayers }
            val winningSide = if (spyCaught) WinningSide.PLAYERS else WinningSide.SPY

            val spyNames = spyIds.mapNotNull { id ->
                players.find { it.id == id }?.nickname
            }

            // Build spy guess options (host-side): all locations from the word pack
            val spyGuessOptions = state?.spyGuessOptions.orEmpty()

            // Am I the spy? Show the guess UI if spy was caught (last chance)
            val iAmSpy = myId in spyIds
            val showSpyGuess = iAmSpy && spyCaught && spyGuessOptions.isNotEmpty()

            _uiState.update {
                it.copy(
                    myPlayerId = myId,
                    isHost = isHost,
                    players = players,
                    winningSide = winningSide,
                    spyNames = spyNames,
                    location = location,
                    scores = state?.scores.orEmpty(),
                    showSpyGuess = showSpyGuess,
                    spyGuessOptions = spyGuessOptions
                )
            }

            when (winningSide) {
                WinningSide.SPY -> soundManager.play(SoundEvent.SPY_ESCAPED)
                WinningSide.PLAYERS -> soundManager.play(SoundEvent.SPY_CAUGHT)
            }

            // If host, broadcast the round result to peers
            if (isHost) {
                networkRepository.broadcastMessage(
                    Message.RoundEnd(
                        winningSide = winningSide,
                        spyIds = spyIds,
                        location = location
                    )
                )
                if (spyGuessOptions.isNotEmpty() && spyCaught) {
                    networkRepository.broadcastMessage(
                        Message.SpyGuessOptions(spyGuessOptions)
                    )
                }
                listenAsHost()
            } else {
                listenAsPeer()
            }
        }
    }

    private suspend fun listenAsHost() {
        networkRepository.hostIncomingMessages.collect { (_, message) ->
            when (message) {
                is Message.SpyGuess -> {
                    val correct = message.guessedLocation.equals(
                        _uiState.value.location, ignoreCase = true
                    )
                    val result = Message.SpyGuess(
                        playerId = message.playerId,
                        guessedLocation = message.guessedLocation,
                        correct = correct
                    )
                    // Broadcast result to all
                    networkRepository.broadcastMessage(result)
                    _uiState.update {
                        it.copy(
                            spyGuessResult = correct,
                            // If spy guessed correctly, flip winning side
                            winningSide = if (correct) WinningSide.SPY
                            else it.winningSide
                        )
                    }
                }
                else -> {}
            }
        }
    }

    private suspend fun listenAsPeer() {
        networkRepository.peerIncomingMessages.collect { message ->
            when (message) {
                is Message.RoundEnd -> {
                    _uiState.update {
                        it.copy(
                            winningSide = message.winningSide,
                            location = message.location
                        )
                    }
                }
                is Message.ScoreUpdate -> {
                    _uiState.update { it.copy(scores = message.scores) }
                }
                is Message.SpyGuessOptions -> {
                    _uiState.update {
                        it.copy(
                            showSpyGuess = _uiState.value.myPlayerId in
                                (sessionRepository.gameState.value?.assignments
                                    ?.filter { a -> a.value.isSpy }?.keys.orEmpty()),
                            spyGuessOptions = message.options
                        )
                    }
                }
                is Message.SpyGuess -> {
                    _uiState.update {
                        it.copy(
                            spyGuessResult = message.correct,
                            winningSide = if (message.correct) WinningSide.SPY
                            else it.winningSide
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun submitSpyGuess(location: String) {
        viewModelScope.launch {
            val msg = Message.SpyGuess(
                _uiState.value.myPlayerId,
                location
            )
            if (_uiState.value.isHost) {
                // Host processes locally
                val correct = location.equals(_uiState.value.location, ignoreCase = true)
                val result = msg.copy(correct = correct)
                networkRepository.broadcastMessage(result)
                _uiState.update {
                    it.copy(
                        spyGuessResult = correct,
                        showSpyGuess = false,
                        winningSide = if (correct) WinningSide.SPY else it.winningSide
                    )
                }
            } else {
                networkRepository.sendToPeer(msg)
                _uiState.update { it.copy(showSpyGuess = false) }
            }
        }
    }
}
