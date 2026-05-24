package uz.angrykitten.spygame.ui.game.voting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import uz.angrykitten.spygame.data.PreferencesRepository
import uz.angrykitten.spygame.data.SessionRepository
import uz.angrykitten.spygame.model.*
import uz.angrykitten.spygame.network.NetworkRepository
import uz.angrykitten.spygame.sound.SoundEvent
import uz.angrykitten.spygame.sound.SoundManager
import javax.inject.Inject

data class VotingUiState(
    val players: List<Player> = emptyList(),
    val votes: Map<String, Int> = emptyMap(), // playerId -> vote count
    val selectedTarget: String? = null,
    val hasVoted: Boolean = false,
    val allVoted: Boolean = false,
    val timerSeconds: Int = 30,
    val myPlayerId: String = "",
    val isHost: Boolean = false,
    val eliminatedId: String? = null,
    val showResult: Boolean = false,
    val votesCast: Int = 0
)

@HiltViewModel
class VotingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val networkRepository: NetworkRepository,
    private val sessionRepository: SessionRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VotingUiState())
    val uiState: StateFlow<VotingUiState> = _uiState.asStateFlow()

    // replay=0 — late subscribers (rotation, navigation re-entry) must not
    // be handed a stale "go to result" signal.
    private val _navigateToResult = MutableSharedFlow<Boolean>(replay = 0)
    val navigateToResult: SharedFlow<Boolean> = _navigateToResult.asSharedFlow()

    private val _navigateBackToGame = MutableSharedFlow<Boolean>(replay = 0)
    val navigateBackToGame: SharedFlow<Boolean> = _navigateBackToGame.asSharedFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val myId = preferencesRepository.playerId.first()
            val state = sessionRepository.gameState.value
            val isHost = state?.room?.hostId == myId
            val players = state?.room?.players.orEmpty()

            _uiState.update {
                it.copy(
                    myPlayerId = myId,
                    isHost = isHost,
                    players = players
                )
            }

            // Start voting timer
            startVotingTimer()

            // Listen for messages based on role
            if (isHost) {
                listenAsHost()
            } else {
                listenAsPeer()
            }
        }
    }

    private suspend fun listenAsHost() {
        networkRepository.hostIncomingMessages.collect { (_, message) ->
            when (message) {
                is Message.VoteCast -> {
                    val currentVotes = _uiState.value.votes.toMutableMap()
                    currentVotes[message.targetId] =
                        (currentVotes[message.targetId] ?: 0) + 1
                    val newCastCount = _uiState.value.votesCast + 1
                    _uiState.update {
                        it.copy(votes = currentVotes, votesCast = newCastCount)
                    }
                    // Broadcast updated vote counts to all peers
                    networkRepository.broadcastMessage(
                        Message.VoteCast(message.voterId, message.targetId)
                    )
                    // Check if all players have voted (excluding self if not yet voted)
                    checkAllVoted()
                }
                else -> {}
            }
        }
    }

    private suspend fun listenAsPeer() {
        networkRepository.peerIncomingMessages.collect { message ->
            when (message) {
                is Message.VoteCast -> {
                    val currentVotes = _uiState.value.votes.toMutableMap()
                    currentVotes[message.targetId] =
                        (currentVotes[message.targetId] ?: 0) + 1
                    _uiState.update { it.copy(votes = currentVotes) }
                }
                is Message.VoteResult -> {
                    timerJob?.cancel()
                    if (message.eliminatedId != null) {
                        soundManager.play(SoundEvent.PLAYER_ELIMINATED)
                    }
                    _uiState.update {
                        it.copy(
                            eliminatedId = message.eliminatedId,
                            showResult = true,
                            allVoted = true,
                            votes = message.votes
                        )
                    }
                    delay(3000)
                    _navigateToResult.emit(true)
                }
                is Message.PhaseChange -> {
                    if (message.newPhase == GamePhase.ROUND_RESULT) {
                        _navigateToResult.emit(true)
                    }
                }
                else -> {}
            }
        }
    }

    private fun startVotingTimer() {
        timerJob = viewModelScope.launch {
            while (_uiState.value.timerSeconds > 0 && isActive) {
                delay(1000)
                _uiState.update { it.copy(timerSeconds = it.timerSeconds - 1) }
            }
            // Auto-tally when timer expires
            if (!_uiState.value.allVoted) {
                tallyVotes()
            }
        }
    }

    fun selectTarget(playerId: String) {
        if (!_uiState.value.hasVoted) {
            _uiState.update { it.copy(selectedTarget = playerId) }
        }
    }

    fun confirmVote() {
        val target = _uiState.value.selectedTarget ?: return
        if (_uiState.value.hasVoted) return

        soundManager.play(SoundEvent.VOTE_CAST)
        _uiState.update { it.copy(hasVoted = true) }

        viewModelScope.launch {
            val myId = _uiState.value.myPlayerId
            val voteMessage = Message.VoteCast(myId, target)

            // Update local vote count
            val currentVotes = _uiState.value.votes.toMutableMap()
            currentVotes[target] = (currentVotes[target] ?: 0) + 1
            val newCastCount = _uiState.value.votesCast + 1
            _uiState.update { it.copy(votes = currentVotes, votesCast = newCastCount) }

            if (_uiState.value.isHost) {
                // Host: broadcast vote to peers and check completion
                networkRepository.broadcastMessage(voteMessage)
                checkAllVoted()
            } else {
                // Peer: send vote to host
                networkRepository.sendToPeer(voteMessage)
            }
        }
    }

    private suspend fun checkAllVoted() {
        val s = _uiState.value
        val totalVoters = s.players.size
        // votesCast counts: host's own vote + received votes from peers
        if (s.votesCast >= totalVoters) {
            tallyVotes()
        }
    }

    private suspend fun tallyVotes() {
        val votes = _uiState.value.votes
        if (votes.isEmpty()) {
            _uiState.update { it.copy(allVoted = true, showResult = true) }
            delay(2000)
            _navigateBackToGame.emit(true)
            return
        }

        val maxVotes = votes.maxOf { it.value }
        val candidates = votes.filter { it.value == maxVotes }

        val eliminatedId = if (candidates.size == 1) candidates.keys.first() else null

        if (eliminatedId != null) {
            soundManager.play(SoundEvent.PLAYER_ELIMINATED)
        }

        _uiState.update {
            it.copy(
                eliminatedId = eliminatedId,
                showResult = true,
                allVoted = true
            )
        }

        // Host broadcasts the result to all peers
        if (_uiState.value.isHost) {
            networkRepository.broadcastMessage(
                Message.VoteResult(eliminatedId = eliminatedId, votes = votes)
            )
        }

        delay(3000)
        _navigateToResult.emit(true)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
