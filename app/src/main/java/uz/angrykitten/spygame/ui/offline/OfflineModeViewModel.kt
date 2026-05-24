package uz.angrykitten.spygame.ui.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.angrykitten.spygame.data.WordPackRepository
import uz.angrykitten.spygame.model.Role
import uz.angrykitten.spygame.model.WordPack
import uz.angrykitten.spygame.sound.SoundEvent
import uz.angrykitten.spygame.sound.SoundManager
import javax.inject.Inject

/** Per-spec phases for the offline reveal sequence. */
enum class OfflineRevealPhase {
    WAITING,   // Pass-to-player prompt; tap card to flip
    VIEWING,   // Role card is face-up
    DONE       // Player tapped "Done"; ready to advance
}

data class OfflineUiState(
    // Setup
    val playerCount: Int = 4,
    val wordPackIndex: Int = 0,
    val numberOfSpies: Int = 1,
    val timerMinutes: Int = 5,

    // Round
    val currentLocation: String = "",
    val assignments: List<Role> = emptyList(),

    // Reveal phase machine
    val currentRevealIndex: Int = 0,
    val revealPhase: OfflineRevealPhase = OfflineRevealPhase.WAITING,
    val allRevealed: Boolean = false,

    // Playing
    val timerSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val isPaused: Boolean = false,
    val eliminated: Set<Int> = emptySet(),
    val locationRevealed: Boolean = false
)

@HiltViewModel
class OfflineModeViewModel @Inject constructor(
    private val wordPackRepository: WordPackRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineUiState())
    val uiState: StateFlow<OfflineUiState> = _uiState.asStateFlow()

    val wordPacks: List<WordPack> = wordPackRepository.getAvailablePacks()

    private var timerJob: Job? = null

    // ─── Setup ────────────────────────────────────────────────────────────

    fun updatePlayerCount(count: Int) {
        val clamped = count.coerceIn(3, 10)
        val maxSpies = (clamped - 1).coerceAtMost(3).coerceAtLeast(1)
        _uiState.update {
            it.copy(
                playerCount = clamped,
                numberOfSpies = it.numberOfSpies.coerceIn(1, maxSpies)
            )
        }
    }

    fun updateWordPack(index: Int) {
        _uiState.update { it.copy(wordPackIndex = index.coerceIn(0, wordPacks.size - 1)) }
    }

    fun updateNumberOfSpies(count: Int) {
        val s = _uiState.value
        val maxSpies = (s.playerCount - 1).coerceAtMost(3).coerceAtLeast(1)
        _uiState.update { it.copy(numberOfSpies = count.coerceIn(1, maxSpies)) }
    }

    fun updateTimerMinutes(minutes: Int) {
        _uiState.update { it.copy(timerMinutes = minutes.coerceIn(1, 15)) }
    }

    /** Roll roles and location for a fresh round. */
    fun beginRound() {
        val s = _uiState.value
        val pack = wordPacks.getOrNull(s.wordPackIndex) ?: return
        // Use randomOrNull(): an empty pack would otherwise throw
        // NoSuchElementException and crash before the round screen renders.
        val location = pack.words.randomOrNull() ?: return

        val spyRole = Role(name = "Spy", description = "You don't know the location. Blend in!", isSpy = true)
        val agentRole = Role(name = "Agent", description = "You know the location. Find the Spy!", isSpy = false)
        val roles = MutableList(s.playerCount) { agentRole }
        val spyCount = s.numberOfSpies.coerceAtMost(s.playerCount - 1).coerceAtLeast(1)
        val spyIndices = (0 until s.playerCount).shuffled().take(spyCount).toSet()
        spyIndices.forEach { roles[it] = spyRole }

        _uiState.update {
            it.copy(
                currentLocation = location,
                assignments = roles,
                currentRevealIndex = 0,
                revealPhase = OfflineRevealPhase.WAITING,
                allRevealed = false,
                timerSeconds = s.timerMinutes * 60,
                totalSeconds = s.timerMinutes * 60,
                isPaused = false,
                eliminated = emptySet(),
                locationRevealed = false
            )
        }
    }

    // ─── Reveal phase machine ─────────────────────────────────────────────

    /** WAITING → VIEWING. The active player tapped the card. */
    fun onTapReveal() {
        val s = _uiState.value
        if (s.revealPhase != OfflineRevealPhase.WAITING) return
        soundManager.play(SoundEvent.ROLE_REVEAL)
        _uiState.update { it.copy(revealPhase = OfflineRevealPhase.VIEWING) }
    }

    /** VIEWING → DONE. */
    fun onDone() {
        val s = _uiState.value
        if (s.revealPhase != OfflineRevealPhase.VIEWING) return
        _uiState.update { it.copy(revealPhase = OfflineRevealPhase.DONE) }
    }

    /** DONE → next WAITING or allRevealed. */
    fun onNext() {
        val s = _uiState.value
        if (s.revealPhase != OfflineRevealPhase.DONE) return
        val nextIndex = s.currentRevealIndex + 1
        if (nextIndex >= s.playerCount) {
            _uiState.update { it.copy(allRevealed = true) }
        } else {
            _uiState.update {
                it.copy(
                    currentRevealIndex = nextIndex,
                    revealPhase = OfflineRevealPhase.WAITING
                )
            }
        }
    }

    /** Current player's role for the reveal screen. */
    fun currentRole(): Role? =
        _uiState.value.assignments.getOrNull(_uiState.value.currentRevealIndex)

    fun currentLocationForPlayer(): String? {
        val role = currentRole() ?: return null
        return if (role.isSpy) null else _uiState.value.currentLocation
    }

    // ─── Playing ──────────────────────────────────────────────────────────

    fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timerSeconds > 0) {
                if (!_uiState.value.isPaused) {
                    delay(1000)
                    if (!_uiState.value.isPaused) {
                        _uiState.update { it.copy(timerSeconds = it.timerSeconds - 1) }
                    }
                } else {
                    delay(200)
                }
            }
        }
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    /** Pause the timer when the app is backgrounded. */
    fun onAppBackgrounded() {
        if (!_uiState.value.isPaused) {
            _uiState.update { it.copy(isPaused = true) }
        }
    }

    fun toggleEliminated(playerSlot: Int) {
        _uiState.update {
            val newSet = if (playerSlot in it.eliminated) it.eliminated - playerSlot
            else it.eliminated + playerSlot
            it.copy(eliminated = newSet)
        }
    }

    fun revealLocation() {
        _uiState.update { it.copy(locationRevealed = true, isPaused = true) }
    }

    fun endRound() {
        timerJob?.cancel()
        _uiState.update { it.copy(isPaused = true) }
    }

    /** Reset for a new round but keep setup choices. */
    fun playAgain() {
        timerJob?.cancel()
        beginRound()
    }

    /** Reset everything when leaving offline mode. */
    fun reset() {
        timerJob?.cancel()
        _uiState.value = OfflineUiState()
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
