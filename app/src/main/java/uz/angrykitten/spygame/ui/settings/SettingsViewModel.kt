package uz.angrykitten.spygame.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.angrykitten.spygame.data.PreferencesRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = preferencesRepository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val soundEnabled: StateFlow<Boolean> = preferencesRepository.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val language: StateFlow<String> = preferencesRepository.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    fun toggleTheme() {
        viewModelScope.launch {
            preferencesRepository.setDarkTheme(!isDarkTheme.value)
        }
    }

    fun toggleSound() {
        viewModelScope.launch {
            preferencesRepository.setSoundEnabled(!soundEnabled.value)
        }
    }

    fun setLanguage(lang: String) {
        if (lang == language.value) return
        viewModelScope.launch {
            preferencesRepository.setLanguage(lang)
            // Apply the locale immediately — AppCompatDelegate will recreate
            // the foreground Activity so every string reloads in the new
            // language without a manual restart.
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(lang)
            )
        }
    }
}
