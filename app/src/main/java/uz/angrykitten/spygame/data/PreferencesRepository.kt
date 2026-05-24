package uz.angrykitten.spygame.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cipher_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val NICKNAME = stringPreferencesKey("nickname")
        val AVATAR_INDEX = intPreferencesKey("avatar_index")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val LANGUAGE = stringPreferencesKey("language")
        val PLAYER_ID = stringPreferencesKey("player_id")
    }

    val nickname: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.NICKNAME] ?: "Player" }

    val avatarIndex: Flow<Int> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.AVATAR_INDEX] ?: 0 }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.IS_DARK_THEME] ?: true }

    val soundEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.SOUND_ENABLED] ?: true }

    val language: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.LANGUAGE] ?: defaultLanguageFromLocale() }

    val playerId: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.PLAYER_ID] ?: "" }

    suspend fun setNickname(nickname: String) {
        context.dataStore.edit { it[Keys.NICKNAME] = nickname }
    }

    suspend fun setAvatarIndex(index: Int) {
        context.dataStore.edit { it[Keys.AVATAR_INDEX] = index }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { it[Keys.IS_DARK_THEME] = isDark }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    suspend fun setPlayerId(id: String) {
        context.dataStore.edit { it[Keys.PLAYER_ID] = id }
    }

    /**
     * Blocking read of the active language for use at application startup
     * (before any Composable runs). English unless the user has explicitly
     * chosen ru/uz, or the device locale is one of those.
     */
    fun currentLanguageBlocking(): String = runBlocking { language.first() }

    private fun defaultLanguageFromLocale(): String =
        when (Locale.getDefault().language) {
            "ru" -> "ru"
            "uz" -> "uz"
            else -> "en"
        }
}
