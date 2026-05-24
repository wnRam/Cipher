package uz.angrykitten.spygame.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val room: Room,
    val phase: GamePhase = GamePhase.LOBBY,
    val currentLocation: String = "",
    val assignments: Map<String, Role> = emptyMap(),
    val timerRemainingSeconds: Int = 300,
    val votes: Map<String, String> = emptyMap(),
    val eliminatedPlayers: List<String> = emptyList(),
    val roundNumber: Int = 1,
    val scores: Map<String, Int> = emptyMap(),
    val spyGuessOptions: List<String> = emptyList()
)

@Serializable
enum class GamePhase {
    LOBBY,
    ROLE_REVEAL,
    PLAYING,
    VOTING,
    ROUND_RESULT,
    SCOREBOARD
}

@Serializable
enum class WinningSide {
    SPY,
    PLAYERS
}
