package uz.angrykitten.spygame.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.angrykitten.spygame.data.PreferencesRepository
import uz.angrykitten.spygame.network.NetworkRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val networkRepository: NetworkRepository
) : ViewModel() {

    val nickname: StateFlow<String> = preferencesRepository.nickname
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Player")

    val avatarIndex: StateFlow<Int> = preferencesRepository.avatarIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Observe real-time WiFi connectivity changes so the banner reflects
    // disconnect/reconnect events while Home is visible. Registration is
    // automatically released when no collectors are active for 5s.
    val isWifiConnected: StateFlow<Boolean> = networkRepository.wifiConnectedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        viewModelScope.launch {
            val currentId = preferencesRepository.playerId.first()
            if (currentId.isEmpty()) {
                preferencesRepository.setPlayerId(UUID.randomUUID().toString())
            }
        }
    }
}
