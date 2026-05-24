package uz.angrykitten.spygame.ui.room.lobby

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.angrykitten.spygame.R
import uz.angrykitten.spygame.data.PreferencesRepository
import uz.angrykitten.spygame.data.SessionRepository
import uz.angrykitten.spygame.model.GamePhase
import uz.angrykitten.spygame.model.GameState
import uz.angrykitten.spygame.model.Message
import uz.angrykitten.spygame.model.Player
import uz.angrykitten.spygame.model.Role
import uz.angrykitten.spygame.network.NetworkRepository
import uz.angrykitten.spygame.util.QRGenerator
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class LobbyUiState(
    val roomCode: String = "",
    val qrContent: String = "",
    val players: List<Player> = emptyList(),
    val isHost: Boolean = false,
    val canStart: Boolean = false,
    val error: String? = null,
    val ready: Boolean = false
)

@HiltViewModel
class LobbyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val networkRepository: NetworkRepository,
    private val sessionRepository: SessionRepository,
    private val qrGenerator: QRGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    private val _qrBitmap = MutableStateFlow<ImageBitmap?>(null)
    val qrBitmap: StateFlow<ImageBitmap?> = _qrBitmap.asStateFlow()

    // replay=0: navigation events are one-shot. A late subscriber after
    // process / config change must not re-trigger navigation.
    private val _gameStarted = MutableSharedFlow<Boolean>(replay = 0)
    val gameStarted: SharedFlow<Boolean> = _gameStarted.asSharedFlow()

    private val _navigateHome = MutableSharedFlow<Boolean>(replay = 0)
    val navigateHome: SharedFlow<Boolean> = _navigateHome.asSharedFlow()

    private var initialized = false
    private var currentGameState: GameState? = null
    /** Guard against rapid double-tap on Start Game. */
    private val startGameTriggered = AtomicBoolean(false)
    private var sourceBitmap: Bitmap? = null

    fun bind(isHost: Boolean) {
        if (initialized) return
        initialized = true
        if (isHost) initAsHost() else initAsPeer()
    }

    private fun initAsHost() {
        val gameState = sessionRepository.gameState.value
        val roomCode = sessionRepository.roomCode.value
        val qrContent = sessionRepository.qrContent.value

        if (gameState == null || roomCode.isBlank()) {
            _uiState.update {
                it.copy(error = "session_missing", ready = true, isHost = true)
            }
            return
        }

        currentGameState = gameState
        _uiState.update {
            it.copy(
                roomCode = roomCode,
                qrContent = qrContent,
                players = gameState.room.players,
                isHost = true,
                canStart = gameState.room.players.size >= 3,
                ready = true
            )
        }

        // Generate the QR bitmap off the main thread. Hand the UI an
        // ImageBitmap so the source Bitmap can be retained once and recycled
        // when the VM is cleared.
        if (qrContent.isNotBlank()) {
            viewModelScope.launch {
                val (bmp, image) = withContext(Dispatchers.IO) {
                    val b = qrGenerator.generateQRCode(qrContent, 480)
                    b to b.asImageBitmap()
                }
                sourceBitmap = bmp
                _qrBitmap.value = image
            }
        }

        viewModelScope.launch {
            networkRepository.hostIncomingMessages.collect { (senderId, message) ->
                when (message) {
                    is Message.PlayerJoined -> handlePlayerJoined(message.player)
                    is Message.PlayerLeft -> handlePlayerLeft(message.playerId)
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            networkRepository.hostClientDisconnected.collect { playerId ->
                handlePlayerLeft(playerId)
            }
        }
    }

    private fun initAsPeer() {
        _uiState.update { it.copy(isHost = false, ready = true) }

        viewModelScope.launch {
            networkRepository.peerIncomingMessages.collect { message ->
                when (message) {
                    is Message.RoomInfo -> {
                        _uiState.update {
                            it.copy(
                                roomCode = message.room.code,
                                players = message.room.players
                            )
                        }
                    }
                    is Message.GameStart -> {
                        currentGameState = message.state
                        sessionRepository.updateGameState(message.state)
                        _gameStarted.emit(true)
                    }
                    is Message.PlayerKicked -> {
                        val myId = preferencesRepository.playerId.first()
                        if (message.playerId == myId) {
                            networkRepository.disconnectPeer()
                            _navigateHome.emit(true)
                        }
                    }
                    is Message.Error -> {
                        _uiState.update { it.copy(error = message.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    private suspend fun handlePlayerJoined(player: Player) {
        val updatedPlayers = (_uiState.value.players + player).distinctBy { it.id }
        val nicknameCounts = mutableMapOf<String, Int>()
        val resolvedPlayers = updatedPlayers.map { p ->
            val count = nicknameCounts.getOrDefault(p.nickname, 0)
            nicknameCounts[p.nickname] = count + 1
            if (count > 0) p.copy(nickname = "${p.nickname}#${count + 1}") else p
        }

        _uiState.update {
            it.copy(players = resolvedPlayers, canStart = resolvedPlayers.size >= 3)
        }
        currentGameState = currentGameState?.let { state ->
            state.copy(room = state.room.copy(players = resolvedPlayers))
        }
        currentGameState?.let { sessionRepository.updateGameState(it) }
        currentGameState?.let {
            networkRepository.broadcastMessage(Message.RoomInfo(it.room))
        }
    }

    private fun handlePlayerLeft(playerId: String) {
        val updatedPlayers = _uiState.value.players.filter { it.id != playerId }
        _uiState.update {
            it.copy(
                players = updatedPlayers,
                canStart = updatedPlayers.size >= 3
            )
        }
        currentGameState = currentGameState?.let { state ->
            state.copy(room = state.room.copy(players = updatedPlayers))
        }
        currentGameState?.let { sessionRepository.updateGameState(it) }
        currentGameState?.let {
            networkRepository.broadcastMessage(Message.RoomInfo(it.room))
        }
    }

    fun kickPlayer(playerId: String) {
        networkRepository.sendToPlayer(playerId, Message.PlayerKicked(playerId))
        networkRepository.kickPlayer(playerId)
        handlePlayerLeft(playerId)
    }

    fun startGame() {
        if (_uiState.value.players.size < 3) return
        // Disable rapid double-tap that could otherwise broadcast GameStart
        // twice and re-roll roles.
        if (!startGameTriggered.compareAndSet(false, true)) return

        viewModelScope.launch {
            val state = currentGameState ?: return@launch
            val players = _uiState.value.players
            val settings = state.room.settings

            val location = settings.wordPack.words.randomOrNull()
            if (location == null) {
                // Empty pack — abort so we don't crash on .random() and free
                // the start-game guard for a retry once the user fixes the pack.
                startGameTriggered.set(false)
                return@launch
            }
            val shuffledPlayerIds = players.map { it.id }.shuffled()
            val assignments = mutableMapOf<String, Role>()

            val spyRole = settings.roles.firstOrNull { it.isSpy }
                ?: Role(
                    name = context.getString(R.string.role_spy),
                    description = context.getString(R.string.role_spy_desc),
                    isSpy = true
                )
            val agentRole = settings.roles.firstOrNull { !it.isSpy }
                ?: Role(
                    name = context.getString(R.string.role_agent),
                    description = context.getString(R.string.role_agent_desc),
                    isSpy = false
                )

            val spyCount = settings.numberOfSpies.coerceAtMost(players.size - 1)
            shuffledPlayerIds.forEachIndexed { index, playerId ->
                assignments[playerId] = if (index < spyCount) spyRole else agentRole
            }

            val newState = state.copy(
                phase = GamePhase.ROLE_REVEAL,
                currentLocation = location,
                assignments = assignments,
                timerRemainingSeconds = settings.timerSeconds,
                scores = players.associate { it.id to (state.scores[it.id] ?: 0) }
            )
            currentGameState = newState
            sessionRepository.updateGameState(newState)

            players.forEach { player ->
                val role = assignments[player.id] ?: agentRole
                val loc = if (role.isSpy) null else location
                networkRepository.sendToPlayer(
                    player.id,
                    Message.RoleAssigned(player.id, role, loc)
                )
            }

            networkRepository.broadcastMessage(Message.GameStart(newState))
            _gameStarted.emit(true)
        }
    }

    fun leaveRoom() {
        viewModelScope.launch {
            if (_uiState.value.isHost) {
                networkRepository.stopHosting()
            } else {
                val myId = preferencesRepository.playerId.first()
                networkRepository.sendToPeer(Message.PlayerLeft(myId))
                networkRepository.disconnectPeer()
            }
            sessionRepository.clear()
        }
    }

    fun retryHostSession() {
        // No-op for now — caller (lobby screen) navigates back to CreateRoom.
    }

    fun getGameState(): GameState? = currentGameState

    override fun onCleared() {
        super.onCleared()
        // Drop the source Bitmap so the ImageBitmap can be GC'd without
        // leaking a 480×480 ARGB_8888 (~921 KB) allocation per host session.
        sourceBitmap?.recycle()
        sourceBitmap = null
        _qrBitmap.value = null
    }
}
