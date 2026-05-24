package uz.angrykitten.spygame.model

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val code: String,
    val hostId: String,
    val players: List<Player> = emptyList(),
    val settings: GameSettings = GameSettings()
)

@Serializable
data class GameSettings(
    val timerSeconds: Int = 300,
    val revealMode: RevealMode = RevealMode.OWN_DEVICE,
    val winConditions: WinConditions = WinConditions(),
    val wordPack: WordPack = WordPack(),
    val roles: List<Role> = listOf(
        Role("Spy", "You don't know the location. Blend in!", isSpy = true, count = 1),
        Role("Agent", "You know the location. Find the Spy!", isSpy = false, count = 1)
    ),
    val numberOfSpies: Int = 1
)

@Serializable
enum class RevealMode {
    OWN_DEVICE,
    PASS_THE_PHONE
}

@Serializable
data class WinConditions(
    val spyWinsByHiding: Boolean = true,
    val spyWinsByGuessing: Boolean = true
)

@Serializable
data class WordPack(
    val name: String = "Locations",
    val words: List<String> = emptyList(),
    val isCustom: Boolean = false
)
