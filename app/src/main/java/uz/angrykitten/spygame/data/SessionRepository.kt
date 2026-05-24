package uz.angrykitten.spygame.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uz.angrykitten.spygame.model.GameState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared in-memory state for the current room session. Filled in by the host
 * when a room is created (or by a peer when joining) so that downstream screens
 * — Lobby, RoleReveal, Game — can read the same data without round-tripping
 * through navigation arguments.
 */
@Singleton
class SessionRepository @Inject constructor() {

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _roomCode = MutableStateFlow("")
    val roomCode: StateFlow<String> = _roomCode.asStateFlow()

    private val _qrContent = MutableStateFlow("")
    val qrContent: StateFlow<String> = _qrContent.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    fun setHostSession(gameState: GameState, roomCode: String, qrContent: String) {
        _gameState.value = gameState
        _roomCode.value = roomCode
        _qrContent.value = qrContent
        _isHost.value = true
    }

    fun setPeerSession(gameState: GameState? = null, roomCode: String = "") {
        _gameState.value = gameState
        _roomCode.value = roomCode
        _qrContent.value = ""
        _isHost.value = false
    }

    fun updateGameState(newState: GameState) {
        _gameState.value = newState
    }

    fun clear() {
        _gameState.value = null
        _roomCode.value = ""
        _qrContent.value = ""
        _isHost.value = false
    }
}
