package uz.angrykitten.spygame.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Message {
    @Serializable
    @SerialName("player_joined")
    data class PlayerJoined(val player: Player) : Message()

    @Serializable
    @SerialName("player_left")
    data class PlayerLeft(val playerId: String) : Message()

    @Serializable
    @SerialName("player_kicked")
    data class PlayerKicked(val playerId: String) : Message()

    @Serializable
    @SerialName("game_start")
    data class GameStart(val state: GameState) : Message()

    @Serializable
    @SerialName("role_assigned")
    data class RoleAssigned(
        val playerId: String,
        val role: Role,
        val location: String?
    ) : Message()

    @Serializable
    @SerialName("timer_update")
    data class TimerUpdate(val remainingSeconds: Int) : Message()

    @Serializable
    @SerialName("vote_cast")
    data class VoteCast(val voterId: String, val targetId: String) : Message()

    @Serializable
    @SerialName("vote_result")
    data class VoteResult(
        val eliminatedId: String?,
        val votes: Map<String, Int>
    ) : Message()

    @Serializable
    @SerialName("phase_change")
    data class PhaseChange(val newPhase: GamePhase) : Message()

    @Serializable
    @SerialName("round_end")
    data class RoundEnd(
        val winningSide: WinningSide,
        val spyIds: List<String>,
        val location: String
    ) : Message()

    @Serializable
    @SerialName("score_update")
    data class ScoreUpdate(val scores: Map<String, Int>) : Message()

    @Serializable
    @SerialName("spy_guess")
    data class SpyGuess(
        val playerId: String,
        val guessedLocation: String,
        val correct: Boolean = false
    ) : Message()

    @Serializable
    @SerialName("spy_guess_options")
    data class SpyGuessOptions(val options: List<String>) : Message()

    @Serializable
    @SerialName("player_ready")
    data class PlayerReady(val playerId: String) : Message()

    @Serializable
    @SerialName("reveal_progress")
    data class RevealProgress(
        val confirmedCount: Int,
        val totalCount: Int
    ) : Message()

    @Serializable
    @SerialName("all_ready")
    data object AllReady : Message()

    @Serializable
    @SerialName("room_info")
    data class RoomInfo(val room: Room) : Message()

    @Serializable
    @SerialName("error")
    data class Error(val message: String) : Message()

    @Serializable
    @SerialName("ping")
    data object Ping : Message()

    @Serializable
    @SerialName("pong")
    data object Pong : Message()
}
