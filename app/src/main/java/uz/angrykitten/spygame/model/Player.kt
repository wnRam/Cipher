package uz.angrykitten.spygame.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * @Immutable tells the Compose compiler that all properties are read-only
 * and that referentially equal instances are structurally equal — so player
 * rows in LazyColumn / VoteCard grids can skip recomposition when the parent
 * passes the same instance back.
 */
@Immutable
@Serializable
data class Player(
    val id: String,
    val nickname: String,
    val avatarIndex: Int = 0,
    val isHost: Boolean = false,
    val score: Int = 0,
    val isEliminated: Boolean = false
)
