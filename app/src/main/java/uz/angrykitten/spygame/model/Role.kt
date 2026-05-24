package uz.angrykitten.spygame.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Role(
    val name: String,
    val description: String,
    val isSpy: Boolean,
    val count: Int = 1
)
