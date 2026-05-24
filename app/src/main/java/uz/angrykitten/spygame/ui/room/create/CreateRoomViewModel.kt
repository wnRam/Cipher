package uz.angrykitten.spygame.ui.room.create

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import uz.angrykitten.spygame.data.WordPackRepository
import uz.angrykitten.spygame.model.GameSettings
import uz.angrykitten.spygame.model.GameState
import uz.angrykitten.spygame.model.Player
import uz.angrykitten.spygame.model.Role
import uz.angrykitten.spygame.model.Room
import uz.angrykitten.spygame.model.WinConditions
import uz.angrykitten.spygame.network.NetworkRepository
import uz.angrykitten.spygame.util.QRGenerator
import uz.angrykitten.spygame.R
import javax.inject.Inject

data class CreateRoomUiState(
    val timerMinutes: Int = 5,
    val spyWinsByHiding: Boolean = true,
    val spyWinsByGuessing: Boolean = true,
    val numberOfSpies: Int = 1,
    val selectedPackIndex: Int = 0,
    val customWords: String = "",
    val customRolesEnabled: Boolean = false,
    val roles: List<Role> = emptyList(),
    val isCreating: Boolean = false,
    @param:androidx.annotation.StringRes val errorResId: Int? = null
)

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val wordPackRepository: WordPackRepository,
    private val networkRepository: NetworkRepository,
    private val sessionRepository: SessionRepository,
    private val qrGenerator: QRGenerator
) : ViewModel() {

    private fun defaultRoles(): List<Role> = listOf(
        Role(
            name = context.getString(R.string.role_spy),
            description = context.getString(R.string.role_spy_desc),
            isSpy = true,
            count = 1
        ),
        Role(
            name = context.getString(R.string.role_agent),
            description = context.getString(R.string.role_agent_desc),
            isSpy = false,
            count = 1
        )
    )

    private val _uiState = MutableStateFlow(CreateRoomUiState(roles = defaultRoles()))
    val uiState: StateFlow<CreateRoomUiState> = _uiState.asStateFlow()

    val wordPacks = wordPackRepository.getAvailablePacks()

    private val _roomCreated = MutableSharedFlow<Boolean>(replay = 0)
    val roomCreated: SharedFlow<Boolean> = _roomCreated.asSharedFlow()

    fun updateTimerMinutes(minutes: Int) {
        _uiState.update { it.copy(timerMinutes = minutes.coerceIn(1, 15)) }
    }

    fun updateSpyWinsByHiding(enabled: Boolean) {
        _uiState.update { it.copy(spyWinsByHiding = enabled) }
    }

    fun updateSpyWinsByGuessing(enabled: Boolean) {
        _uiState.update { it.copy(spyWinsByGuessing = enabled) }
    }

    fun updateNumberOfSpies(count: Int) {
        _uiState.update { it.copy(numberOfSpies = count.coerceIn(1, 3)) }
    }

    fun updateSelectedPack(index: Int) {
        _uiState.update { it.copy(selectedPackIndex = index) }
    }

    fun updateCustomWords(words: String) {
        _uiState.update { it.copy(customWords = words) }
    }

    fun toggleCustomRoles() {
        _uiState.update { it.copy(customRolesEnabled = !it.customRolesEnabled) }
    }

    fun addRole() {
        _uiState.update {
            it.copy(
                roles = it.roles + Role(
                    name = context.getString(R.string.role_new),
                    description = "",
                    isSpy = false
                )
            )
        }
    }

    fun removeRole(index: Int) {
        _uiState.update {
            if (it.roles.size > 2) {
                it.copy(roles = it.roles.filterIndexed { i, _ -> i != index })
            } else it
        }
    }

    fun updateRole(index: Int, role: Role) {
        _uiState.update {
            it.copy(roles = it.roles.toMutableList().apply { set(index, role) })
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorResId = null) }
    }

    fun createRoom() {
        if (_uiState.value.isCreating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorResId = null) }

            try {
                val state = _uiState.value
                val playerId = preferencesRepository.playerId.first()
                val nickname = preferencesRepository.nickname.first()
                val avatarIndex = preferencesRepository.avatarIndex.first()
                val ip = networkRepository.getLocalIpAddress()
                    ?: throw IllegalStateException("no_wifi")

                val wordPack = if (state.selectedPackIndex < wordPacks.size) {
                    wordPacks[state.selectedPackIndex]
                } else {
                    val customWordList = state.customWords.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    if (customWordList.size < 5) {
                        _uiState.update { it.copy(isCreating = false, errorResId = R.string.need_min_words) }
                        return@launch
                    }
                    wordPackRepository.createCustomPack(
                        context.getString(R.string.custom_pack_name),
                        customWordList
                    )
                }

                // Single-shot host setup: open the socket, derive a stable
                // room code from the bound port, register NSD with that code.
                networkRepository.stopHosting() // be defensive on retry
                val port = networkRepository.startServer()
                val roomCode = networkRepository.generateRoomCode(ip, port)
                networkRepository.registerNsd(port, roomCode)

                val hostPlayer = Player(
                    id = playerId,
                    nickname = nickname,
                    avatarIndex = avatarIndex,
                    isHost = true
                )

                val room = Room(
                    code = roomCode,
                    hostId = playerId,
                    players = listOf(hostPlayer),
                    settings = GameSettings(
                        timerSeconds = state.timerMinutes * 60,
                        winConditions = WinConditions(
                            spyWinsByHiding = state.spyWinsByHiding,
                            spyWinsByGuessing = state.spyWinsByGuessing
                        ),
                        wordPack = wordPack,
                        roles = state.roles,
                        numberOfSpies = state.numberOfSpies
                    )
                )

                val gameState = GameState(room = room)
                val qrPayload = qrGenerator.encodeRoomInfo(ip, port, roomCode)

                sessionRepository.setHostSession(gameState, roomCode, qrPayload)

                _uiState.update { it.copy(isCreating = false) }
                _roomCreated.emit(true)
            } catch (e: Exception) {
                networkRepository.stopHosting()
                val errRes = if (e.message == "no_wifi") R.string.no_wifi
                    else R.string.room_create_failed
                _uiState.update {
                    it.copy(isCreating = false, errorResId = errRes)
                }
            }
        }
    }
}
