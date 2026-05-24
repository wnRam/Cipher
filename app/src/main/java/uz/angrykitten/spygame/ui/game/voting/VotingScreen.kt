package uz.angrykitten.spygame.ui.game.voting

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.angrykitten.spygame.R
import uz.angrykitten.spygame.model.Player
import uz.angrykitten.spygame.ui.components.AvatarIcons
import uz.angrykitten.spygame.ui.theme.*

private val avatarIcons = AvatarIcons

@Composable
fun VotingScreen(
    onNavigateToResult: () -> Unit,
    onNavigateBackToGame: () -> Unit,
    viewModel: VotingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToResult.collect { if (it) onNavigateToResult() }
    }
    LaunchedEffect(Unit) {
        viewModel.navigateBackToGame.collect { if (it) onNavigateBackToGame() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = stringResource(R.string.voting_phase),
            style = MaterialTheme.typography.displaySmall.copy(
                letterSpacing = 6.sp
            ),
            color = AlertRed,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Timer
        Text(
            text = "%02d".format(uiState.timerSeconds),
            style = MaterialTheme.typography.headlineLarge,
            color = if (uiState.timerSeconds <= 10) AlertRed
            else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.select_suspect),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Result display
        if (uiState.showResult) {
            VoteResultDisplay(
                eliminatedId = uiState.eliminatedId,
                players = uiState.players
            )
        } else {
            // Player voting grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                val votablePlayers = uiState.players.filter { it.id != uiState.myPlayerId }
                items(votablePlayers, key = { it.id }) { player ->
                    VoteCard(
                        player = player,
                        voteCount = uiState.votes[player.id] ?: 0,
                        isSelected = uiState.selectedTarget == player.id,
                        isVotingLocked = uiState.hasVoted,
                        onClick = { viewModel.selectTarget(player.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm vote button
            Button(
                onClick = { viewModel.confirmVote() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = uiState.selectedTarget != null && !uiState.hasVoted,
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
            ) {
                Icon(Icons.Filled.HowToVote, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.hasVoted) stringResource(R.string.waiting_for_votes)
                    else stringResource(R.string.confirm_vote),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun VoteCard(
    player: Player,
    voteCount: Int,
    isSelected: Boolean,
    isVotingLocked: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(300),
        label = "borderColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .then(
                if (isSelected) Modifier.border(
                    2.dp, borderColor, RoundedCornerShape(16.dp)
                ) else Modifier
            )
            .clickable(enabled = !isVotingLocked) { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        tonalElevation = if (isSelected) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = avatarIcons.getOrElse(player.avatarIndex) { Icons.Filled.Person },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = player.nickname,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            if (voteCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AlertRed.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = stringResource(R.string.votes_label, voteCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = AlertRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VoteResultDisplay(
    eliminatedId: String?,
    players: List<Player>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        if (eliminatedId != null) {
            val player = players.find { it.id == eliminatedId }
            Icon(
                imageVector = Icons.Filled.PersonRemove,
                contentDescription = null,
                tint = AlertRed,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = player?.nickname ?: "???",
                style = MaterialTheme.typography.headlineMedium,
                color = AlertRed,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.eliminated),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Balance,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.no_elimination),
                style = MaterialTheme.typography.headlineSmall,
                color = WarningAmber,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun VotingScreenDarkPreview() {
    CipherTheme(darkTheme = true) {
        VotingScreen(onNavigateToResult = {}, onNavigateBackToGame = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun VotingScreenLightPreview() {
    CipherTheme(darkTheme = false) {
        VotingScreen(onNavigateToResult = {}, onNavigateBackToGame = {})
    }
}
