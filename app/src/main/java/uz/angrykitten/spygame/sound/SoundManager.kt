package uz.angrykitten.spygame.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import uz.angrykitten.spygame.data.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

enum class SoundEvent {
    ROLE_REVEAL,
    TIMER_TICK,
    TIMER_END,
    VOTE_CAST,
    PLAYER_ELIMINATED,
    SPY_CAUGHT,
    SPY_ESCAPED,
    BUTTON_CLICK
}

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context,
    preferencesRepository: PreferencesRepository
) {
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<SoundEvent, Int>()
    private var isInitialized = false

    // Cached, non-blocking view of the sound-enabled preference. Previously
    // every call to play() did a runBlocking flow read — that pulled the
    // entire DataStore IO pipeline onto the main thread (timer ticks fire
    // every second during a round). Now we read a StateFlow synchronously.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val soundEnabledFlow: StateFlow<Boolean> =
        preferencesRepository.soundEnabled
            .stateIn(scope, SharingStarted.Eagerly, true)

    fun initialize() {
        if (isInitialized) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        // No audio assets ship with the app yet; SoundPool initialised so
        // future loads can land here without re-creating it per play().
        isInitialized = true
    }

    fun play(event: SoundEvent) {
        if (!soundEnabledFlow.value) return
        if (!isInitialized) initialize()
        soundIds[event]?.let { id ->
            soundPool?.play(id, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    /** Pause all active streams when the app is backgrounded. */
    fun autoPause() {
        soundPool?.autoPause()
    }

    /** Resume previously paused streams when the app returns to foreground. */
    fun autoResume() {
        soundPool?.autoResume()
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
        isInitialized = false
    }
}
