package uz.angrykitten.spygame.ui.game.reveal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.angrykitten.spygame.data.PreferencesRepository
import uz.angrykitten.spygame.data.SessionRepository
import uz.angrykitten.spygame.model.GamePhase
import uz.angrykitten.spygame.model.Message
import uz.angrykitten.spygame.model.Player
import uz.angrykitten.spygame.model.RevealMode
import uz.angrykitten.spygame.model.Role
import uz.angrykitten.spygame.network.NetworkRepository
import uz.angrykitten.spygame.sound.SoundEvent
import uz.angrykitten.spygame.sound.SoundManager
import javax.inject.Inject

/** Per-spec phases for the pass-the-phone reveal sequence. */
enum class RevealPhase {
    HAND_OFF,       // "Pass the phone to X" — waits for receiving player to tap I'm ready
    HIDDEN,         // 1.5s black/locked screen while phone changes hands
    VIEWING,        // Current player sees their role card
    CONFIRMED,      // Brief acknowledgment, auto-advances after 2s
    ALL_DONE        // Every player has confirmed — host can Begin Mission
}

data class RoleRevealUiState(
    // Shared
    val isHost: Boolean = false,
    val revealMode: RevealMode = RevealMode.OWN_DEVICE,
    val roundNumber: Int = 1,

    // OWN_DEVICE flow
    val isRevealed: Boolean = false,
    val isReady: Boolean = false,
    val myRole: Role? = null,
    val myLocation: String? = null,
    val readyCount: Int = 0,
    val totalCount: Int = 0,
    val allReady: Boolean = false,

    // PASS_THE_PHONE host-side flow
    val revealPhase: RevealPhase = RevealPhase.HAND_OFF,
    val orderedPlayers: List<Player> = emptyList(),
    val currentRevealIndex: Int = 0,
    val confirmedPlayerIds: Set<String> = emptySet(),
    val currentRole: Role? = null,
    val currentLocation: String? = null,
    val currentIsSelf: Boolean = false,

    // PASS_THE_PHONE peer-side
    val waitingForHost: Boolean = false,
    val progressConfirmed: Int = 0,
    val progressTotal: Int = 0
)

@HiltViewModel
class RoleRevealViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val networkRepository: NetworkRepository,
    private val sessionRepository: SessionRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoleRevealUiState())
    val uiState: StateFlow<RoleRevealUiState> = _uiState.asStateFlow()

    private val _navigateToGame = MutableSharedFlow<Boolean>(replay = 0)
    val navigateToGame: SharedFlow<Boolean> = _navigateToGame.asSharedFlow()

    private var myPlayerId = ""
    private var location: String = ""
    private var assignments: Map<String, Role> = emptyMap()

    init {
        viewModelScope.launch {
            myPlayerId = preferencesRepository.playerId.first()
            initFromSession()
            listenForMessages()
        }
    }

    private fun initFromSession() {
        val state = sessionRepository.gameState.value ?: return
        val players = state.room.players
        val isHost = state.room.hostId == myPlayerId
        val mode = state.room.settings.revealMode
        assignments = state.assignments
        location = state.currentLocation

        when {
            mode == RevealMode.PASS_THE_PHONE && isHost -> {
                // Fix the reveal order once, randomly. The spy lands wherever
                // shuffle puts them.
                val ordered = players.shuffled()
                val first = ordered.firstOrNull()
                _uiState.update {
                    it.copy(
                        isHost = true,
                        revealMode = RevealMode.PASS_THE_PHONE,
                        orderedPlayers = ordered,
                        currentRevealIndex = 0,
                        revealPhase = RevealPhase.HAND_OFF,
                        currentIsSelf = first?.id == myPlayerId,
                        totalCount = ordered.size,
                        roundNumber = state.roundNumber
                    )
                }
            }
            mode == RevealMode.PASS_THE_PHONE && !isHost -> {
                _uiState.update {
                    it.copy(
                        isHost = false,
                        revealMode = RevealMode.PASS_THE_PHONE,
                        waitingForHost = true,
                        progressTotal = players.size,
                        progressConfirmed = 0,
                        roundNumber = state.roundNumber
                    )
                }
            }
            mode == RevealMode.OWN_DEVICE && isHost -> {
                val role = assignments[myPlayerId]
                val loc = if (role?.isSpy == true) null else location
                _uiState.update {
                    it.copy(
                        isHost = true,
                        revealMode = RevealMode.OWN_DEVICE,
                        myRole = role,
                        myLocation = loc,
                        totalCount = players.size,
                        roundNumber = state.roundNumber
                    )
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        isHost = false,
                        revealMode = RevealMode.OWN_DEVICE,
                        totalCount = players.size,
                        roundNumber = state.roundNumber
                    )
                }
            }
        }
    }

    private suspend fun listenForMessages() {
        if (_uiState.value.isHost) {
            networkRepository.hostIncomingMessages.collect { (_, message) ->
                when (message) {
                    is Message.PlayerReady -> handlePeerReady()
                    else -> {}
                }
            }
        } else {
            networkRepository.peerIncomingMessages.collect { message ->
                when (message) {
                    is Message.RoleAssigned -> {
                        if (message.playerId == myPlayerId) {
                            _uiState.update {
                                it.copy(myRole = message.role, myLocation = message.location)
                            }
                        }
                    }
                    is Message.RevealProgress -> {
                        _uiState.update {
                            it.copy(
                                progressConfirmed = message.confirmedCount,
                                progressTotal = message.totalCount
                            )
                        }
                    }
                    is Message.PhaseChange -> {
                        if (message.newPhase == GamePhase.PLAYING) {
                            _navigateToGame.emit(true)
                        }
                    }
                    is Message.AllReady -> _uiState.update { it.copy(allReady = true) }
                    else -> {}
                }
            }
        }
    }

    private fun handlePeerReady() {
        val newCount = _uiState.value.readyCount + 1
        val needed = (_uiState.value.totalCount - 1).coerceAtLeast(0)
        val hostSelfReady = _uiState.value.isReady ||
            _uiState.value.revealMode == RevealMode.PASS_THE_PHONE
        _uiState.update {
            it.copy(
                readyCount = newCount,
                allReady = newCount >= needed && hostSelfReady
            )
        }
    }

    // ─── OWN_DEVICE flow ────────────────────────────────────────────────────

    /** OWN_DEVICE: flip the card face-up. */
    fun revealRole() {
        if (_uiState.value.revealMode != RevealMode.OWN_DEVICE) return
        soundManager.play(SoundEvent.ROLE_REVEAL)
        _uiState.update { it.copy(isRevealed = true) }
    }

    /** OWN_DEVICE: "I've seen my role". */
    fun confirmSeen() {
        if (_uiState.value.revealMode != RevealMode.OWN_DEVICE) return
        _uiState.update { it.copy(isReady = true) }
        viewModelScope.launch {
            if (_uiState.value.isHost) handlePeerReady()
            else networkRepository.sendToPeer(Message.PlayerReady(myPlayerId))
        }
    }

    // ─── PASS_THE_PHONE state machine ──────────────────────────────────────

    /** HAND_OFF → HIDDEN. The receiving player tapped "I'm ready". */
    fun onPlayerReady() {
        val s = _uiState.value
        if (s.revealMode != RevealMode.PASS_THE_PHONE || !s.isHost) return
        if (s.revealPhase != RevealPhase.HAND_OFF) return
        _uiState.update { it.copy(revealPhase = RevealPhase.HIDDEN) }
    }

    /** HIDDEN → VIEWING. Fired by the screen after the 1.5s blackout. */
    fun onHiddenComplete() {
        val s = _uiState.value
        if (s.revealMode != RevealMode.PASS_THE_PHONE || !s.isHost) return
        if (s.revealPhase != RevealPhase.HIDDEN) return
        val player = s.orderedPlayers.getOrNull(s.currentRevealIndex) ?: return
        val role = assignments[player.id]
        val loc = if (role?.isSpy == true) null else location
        soundManager.play(SoundEvent.ROLE_REVEAL)
        _uiState.update {
            it.copy(
                revealPhase = RevealPhase.VIEWING,
                currentRole = role,
                currentLocation = loc
            )
        }
    }

    /** VIEWING → CONFIRMED. Player tapped "I've seen my role" (after the
     *  3-second enforced delay handled in the UI). */
    fun onRoleConfirmed() {
        val s = _uiState.value
        if (s.revealMode != RevealMode.PASS_THE_PHONE || !s.isHost) return
        if (s.revealPhase != RevealPhase.VIEWING) return
        val player = s.orderedPlayers.getOrNull(s.currentRevealIndex) ?: return

        val newConfirmed = s.confirmedPlayerIds + player.id
        _uiState.update {
            it.copy(
                revealPhase = RevealPhase.CONFIRMED,
                confirmedPlayerIds = newConfirmed,
                currentRole = null,
                currentLocation = null
            )
        }
        // Broadcast progress to peer waiting screens.
        viewModelScope.launch {
            networkRepository.broadcastMessage(
                Message.RevealProgress(
                    confirmedCount = newConfirmed.size,
                    totalCount = s.orderedPlayers.size
                )
            )
        }
    }

    /** CONFIRMED auto-advance → next HAND_OFF or ALL_DONE. */
    fun onConfirmedComplete() {
        val s = _uiState.value
        if (s.revealMode != RevealMode.PASS_THE_PHONE || !s.isHost) return
        if (s.revealPhase != RevealPhase.CONFIRMED) return
        val nextIndex = s.currentRevealIndex + 1
        if (nextIndex >= s.orderedPlayers.size) {
            _uiState.update {
                it.copy(
                    revealPhase = RevealPhase.ALL_DONE,
                    allReady = true,
                    currentRevealIndex = nextIndex
                )
            }
        } else {
            val nextPlayer = s.orderedPlayers[nextIndex]
            _uiState.update {
                it.copy(
                    revealPhase = RevealPhase.HAND_OFF,
                    currentRevealIndex = nextIndex,
                    currentIsSelf = nextPlayer.id == myPlayerId
                )
            }
        }
    }

    /** ALL_DONE: host commits to start the game. */
    fun onBeginMission() {
        viewModelScope.launch {
            networkRepository.broadcastMessage(Message.PhaseChange(GamePhase.PLAYING))
            _navigateToGame.emit(true)
        }
    }

    /** Spec: if app is backgrounded during VIEWING, drop back to HAND_OFF for
     *  the same player so they have to re-confirm presence. */
    fun onAppBackgroundedDuringViewing() {
        val s = _uiState.value
        if (s.revealMode == RevealMode.PASS_THE_PHONE &&
            s.isHost &&
            s.revealPhase == RevealPhase.VIEWING
        ) {
            _uiState.update {
                it.copy(
                    revealPhase = RevealPhase.HAND_OFF,
                    currentRole = null,
                    currentLocation = null
                )
            }
        }
    }

    /** Compatibility for older host start flow. */
    fun startGameAsHost() = onBeginMission()
}
