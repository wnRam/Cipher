package uz.angrykitten.spygame.ui.room.join

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.angrykitten.spygame.R
import uz.angrykitten.spygame.data.PreferencesRepository
import uz.angrykitten.spygame.model.Player
import uz.angrykitten.spygame.network.NetworkRepository
import uz.angrykitten.spygame.util.QRGenerator
import javax.inject.Inject

data class JoinRoomUiState(
    val code: String = "",
    val isJoining: Boolean = false,
    @param:StringRes val errorResId: Int? = null
)

@HiltViewModel
class JoinRoomViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val networkRepository: NetworkRepository,
    private val qrGenerator: QRGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinRoomUiState())
    val uiState: StateFlow<JoinRoomUiState> = _uiState.asStateFlow()

    private val _roomJoined = MutableSharedFlow<Boolean>(replay = 0)
    val roomJoined: SharedFlow<Boolean> = _roomJoined.asSharedFlow()

    init {
        // Start NSD discovery
        networkRepository.nsdHelper.startDiscovery()
    }

    fun updateCode(code: String) {
        if (code.length <= 6 && code.all { it.isDigit() }) {
            _uiState.update { it.copy(code = code, errorResId = null) }
            if (code.length == 6) {
                joinByCode(code)
            }
        }
    }

    fun onQRScanned(data: String) {
        val info = qrGenerator.decodeRoomInfo(data)
        if (info != null) {
            joinByAddress(info.ip, info.port)
        } else {
            _uiState.update { it.copy(errorResId = R.string.invalid_qr_code) }
        }
    }

    private fun joinByCode(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, errorResId = null) }

            // Poll NSD discovery for up to 10 seconds with 500ms intervals
            var service = networkRepository.nsdHelper.findServiceByCode(code)
            var elapsed = 0L
            while (service == null && elapsed < 10_000L) {
                delay(500)
                elapsed += 500
                service = networkRepository.nsdHelper.findServiceByCode(code)
            }

            if (service != null) {
                @Suppress("DEPRECATION") // NsdServiceInfo.host is API < 34 fallback
                val host = service.host?.hostAddress
                val port = service.port
                if (host != null) {
                    joinByAddress(host, port)
                    return@launch
                }
            }

            _uiState.update { it.copy(isJoining = false, errorResId = R.string.room_not_found) }
        }
    }

    private fun joinByAddress(host: String, port: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, errorResId = null) }

            val playerId = preferencesRepository.playerId.first()
            val nickname = preferencesRepository.nickname.first()
            val avatarIndex = preferencesRepository.avatarIndex.first()

            val player = Player(
                id = playerId,
                nickname = nickname,
                avatarIndex = avatarIndex
            )

            val success = networkRepository.joinRoom(host, port, player)
            if (success) {
                networkRepository.nsdHelper.stopDiscovery()
                _roomJoined.emit(true)
            } else {
                _uiState.update {
                    it.copy(isJoining = false, errorResId = R.string.connection_failed)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkRepository.nsdHelper.stopDiscovery()
    }
}
