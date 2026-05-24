package uz.angrykitten.spygame.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.angrykitten.spygame.data.PreferencesRepository
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val nickname: StateFlow<String> = preferencesRepository.nickname
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Player")

    val avatarIndex: StateFlow<Int> = preferencesRepository.avatarIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _saveSuccess = MutableSharedFlow<Boolean>(replay = 0)
    val saveSuccess: SharedFlow<Boolean> = _saveSuccess.asSharedFlow()

    fun saveProfile(nickname: String, avatarIndex: Int) {
        viewModelScope.launch {
            if (nickname.isBlank()) return@launch
            if (nickname.length > 20) return@launch
            preferencesRepository.setNickname(nickname.trim())
            preferencesRepository.setAvatarIndex(avatarIndex)
            _saveSuccess.emit(true)
        }
    }
}
