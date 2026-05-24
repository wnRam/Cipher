package uz.angrykitten.spygame.ui.game.playing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import uz.angrykitten.spygame.data.PreferencesRepository
import uz.angrykitten.spygame.data.SessionRepository
import uz.angrykitten.spygame.model.GamePhase
import uz.angrykitten.spygame.model.Message
import uz.angrykitten.spygame.model.Player
import uz.angrykitten.spygame.network.NetworkRepository
import uz.angrykitten.spygame.sound.SoundEvent
import uz.angrykitten.spygame.sound.SoundManager
import javax.inject.Inject

/**
 * Heavy, low-frequency state for the game screen (round, players, host flag).
 * Kept in a dedicated flow so the per-second timer tick does NOT force the
 * player list / action buttons to recompose.
 */
data class GameUiState(
    val roundNumber: Int = 1,
    val totalSeconds: Int = 300,
    val players: List<Player> = emptyList(),
    val eliminatedPlayers: List<String> = emptyList(),
    val isHost: Boolean = false,
    val myPlayerId: String = ""
)

/**
 * High-frequency timer state — exposed separately so only the TimerDisplay
 * composable recomposes on every tick.
 */
data class GameTimerState(
    val timerSeconds: Int = 300,
    val isPaused: Boolean = false
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val networkRepository: NetworkRepository,
    private val sessionRepository: SessionRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _timerState = MutableStateFlow(GameTimerState())
    val timerState: StateFlow<GameTimerState> = _timerState.asStateFlow()

    // SharedFlow for one-shot navigation events. replay=0 so a fresh subscriber
    // (e.g. after rotation) does not receive a stale "go to voting" signal.
    private val _navigateToVoting = MutableSharedFlow<Boolean>(replay = 0)
    val navigateToVoting: SharedFlow<Boolean> = _navigateToVoting.asSharedFlow()

    private val _navigateToResult = MutableSharedFlow<Boolean>(replay = 0)
    val navigateToResult: SharedFlow<Boolean> = _navigateToResult.asSharedFlow()

    /** Guards against rapid double-tap on Call Vote / End Round. */
    private val voteCalled = AtomicBoolean(false)
    private val roundEnded = AtomicBoolean(false)

    private var timerJob: Job? = null

    // Tracks whether the timer was running before the app was backgrounded,
    // so we can resume it on foreground without overriding a host-initiated pause.
    private var wasPausedBeforeBackground: Boolean = false

    init {
        viewModelScope.launch {
            val myId = preferencesRepository.playerId.first()
            val state = sessionRepository.gameState.value
            val isHost = state?.room?.hostId == myId

            val totalSeconds = state?.room?.settings?.timerSeconds ?: 300
            val remaining = state?.timerRemainingSeconds?.takeIf { it > 0 } ?: totalSeconds

            _uiState.update {
                it.copy(
                    myPlayerId = myId,
                    isHost = isHost,
                    totalSeconds = totalSeconds,
                    players = state?.room?.players.orEmpty(),
                    eliminatedPlayers = state?.eliminatedPlayers.orEmpty(),
                    roundNumber = state?.roundNumber ?: 1
                )
            }
            _timerState.update { it.copy(timerSeconds = remaining) }

            if (isHost) {
                startTimer()
                listenAsHost()
            } else {
                listenAsPeer()
            }
        }
    }

    private suspend fun listenAsPeer() {
        networkRepository.peerIncomingMessages.collect { message ->
            when (message) {
                is Message.TimerUpdate -> {
                    _timerState.update { it.copy(timerSeconds = message.remainingSeconds) }
                    if (message.remainingSeconds in 1..10) {
                        soundManager.play(SoundEvent.TIMER_TICK)
                    }
                    if (message.remainingSeconds == 0) {
                        soundManager.play(SoundEvent.TIMER_END)
                    }
                }
                is Message.PhaseChange -> when (message.newPhase) {
                    GamePhase.VOTING -> _navigateToVoting.emit(true)
                    GamePhase.ROUND_RESULT -> _navigateToResult.emit(true)
                    else -> {}
                }
                is Message.PlayerLeft -> {
                    _uiState.update {
                        it.copy(players = it.players.filter { p -> p.id != message.playerId })
                    }
                }
                else -> {}
            }
        }
    }

    private suspend fun listenAsHost() {
        // Peers don't send timer info to the host; we still listen for player
        // departures and any peer-initiated phase changes.
        networkRepository.hostIncomingMessages.collect { (_, message) ->
            when (message) {
                is Message.PlayerLeft -> {
                    _uiState.update {
                        it.copy(players = it.players.filter { p -> p.id != message.playerId })
                    }
                }
                is Message.PhaseChange -> when (message.newPhase) {
                    GamePhase.VOTING -> {
                        networkRepository.broadcastMessage(Message.PhaseChange(GamePhase.VOTING))
                        _navigateToVoting.emit(true)
                    }
                    else -> {}
                }
                else -> {}
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            // Broadcast the starting value once so peers sync immediately.
            networkRepository.broadcastMessage(
                Message.TimerUpdate(_timerState.value.timerSeconds)
            )
            while (isActive && _timerState.value.timerSeconds > 0) {
                delay(1000)
                if (_timerState.value.isPaused) continue
                val next = _timerState.value.timerSeconds - 1
                _timerState.update { it.copy(timerSeconds = next) }
                networkRepository.broadcastMessage(Message.TimerUpdate(next))
                if (next in 1..10) soundManager.play(SoundEvent.TIMER_TICK)
                if (next == 0) {
                    soundManager.play(SoundEvent.TIMER_END)
                    networkRepository.broadcastMessage(
                        Message.PhaseChange(GamePhase.ROUND_RESULT)
                    )
                    _navigateToResult.emit(true)
                    break
                }
            }
        }
    }

    fun pauseTimer() {
        _timerState.update { it.copy(isPaused = !it.isPaused) }
    }

    /**
     * Pause the timer when the app is backgrounded. The host's local timer
     * is the source of truth for peers, so pausing here prevents the round
     * draining while no one is watching. Peers also pause their own
     * displayed countdown until the next TimerUpdate broadcast.
     */
    fun onAppBackgrounded() {
        wasPausedBeforeBackground = _timerState.value.isPaused
        if (!_timerState.value.isPaused) {
            _timerState.update { it.copy(isPaused = true) }
        }
    }

    fun onAppForegrounded() {
        // Only resume if we were the ones who paused (don't override a
        // host-initiated pause that existed before backgrounding).
        if (!wasPausedBeforeBackground && _timerState.value.isPaused) {
            _timerState.update { it.copy(isPaused = false) }
        }
    }

    fun callVote() {
        if (!voteCalled.compareAndSet(false, true)) return
        viewModelScope.launch {
            if (_uiState.value.isHost) {
                networkRepository.broadcastMessage(Message.PhaseChange(GamePhase.VOTING))
            } else {
                networkRepository.sendToPeer(Message.PhaseChange(GamePhase.VOTING))
            }
            _navigateToVoting.emit(true)
        }
    }

    fun endRound() {
        if (!roundEnded.compareAndSet(false, true)) return
        timerJob?.cancel()
        viewModelScope.launch {
            networkRepository.broadcastMessage(Message.PhaseChange(GamePhase.ROUND_RESULT))
            _navigateToResult.emit(true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
