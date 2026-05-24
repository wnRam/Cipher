package uz.angrykitten.spygame.ui.game.playing

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.angrykitten.spygame.R
import uz.angrykitten.spygame.model.Player
import uz.angrykitten.spygame.ui.components.AvatarIcons
import uz.angrykitten.spygame.ui.theme.*

@Composable
fun GameScreen(
    onNavigateToVoting: () -> Unit,
    onNavigateToResult: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Timer state is consumed only by TimerDisplay so the player list and
    // action buttons do NOT recompose every second.

    LaunchedEffect(Unit) {
        viewModel.navigateToVoting.collect { if (it) onNavigateToVoting() }
    }
    LaunchedEffect(Unit) {
        viewModel.navigateToResult.collect { if (it) onNavigateToResult() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                Lifecycle.Event.ON_START -> viewModel.onAppForegrounded()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Round number
            Text(
                text = stringResource(R.string.round_number, uiState.roundNumber),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Isolated scope: the StateFlow read happens inside TimerDisplay's
            // own composition, so per-second updates do not invalidate the
            // surrounding column.
            TimerDisplay(
                timerStateFlow = viewModel.timerState,
                totalSeconds = uiState.totalSeconds
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Player list
            Text(
                text = stringResource(R.string.players_joined, uiState.players.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(uiState.players, key = { it.id }) { player ->
                    val isEliminated = player.id in uiState.eliminatedPlayers
                    GamePlayerItem(player = player, isEliminated = isEliminated)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Call vote button
                Button(
                    onClick = { viewModel.callVote() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlertRed
                    )
                ) {
                    Icon(Icons.Filled.HowToVote, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.call_vote),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Host controls — only reads isPaused, scoped to its own block.
            if (uiState.isHost) {
                Spacer(modifier = Modifier.height(8.dp))
                HostTimerControls(
                    timerStateFlow = viewModel.timerState,
                    onPauseToggle = viewModel::pauseTimer,
                    onEndRound = viewModel::endRound
                )
            }
        }
    }
}

@Composable
private fun TimerDisplay(
    timerStateFlow: kotlinx.coroutines.flow.StateFlow<GameTimerState>,
    totalSeconds: Int
) {
    // State is read here so only this composable invalidates on tick.
    val timer by timerStateFlow.collectAsStateWithLifecycle()
    val seconds = timer.timerSeconds
    val isPaused = timer.isPaused
    val minutes = seconds / 60
    val secs = seconds % 60
    val progress = if (totalSeconds > 0) {
        (seconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val timerColor by animateColorAsState(
        targetValue = when {
            seconds <= 60 -> AlertRed
            seconds <= 120 -> WarningAmber
            else -> NeonGreen
        },
        animationSpec = tween(500),
        label = "timerColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(180.dp)
            .drawBehind {
                // Background ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    style = Stroke(width = 12f)
                )
                // Progress ring
                drawArc(
                    color = timerColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 12f, cap = StrokeCap.Round)
                )
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%02d:%02d".format(minutes, secs),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = timerColor
            )
            if (isPaused) {
                Text(
                    text = stringResource(R.string.paused),
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningAmber
                )
            }
        }
    }
}

@Composable
private fun HostTimerControls(
    timerStateFlow: kotlinx.coroutines.flow.StateFlow<GameTimerState>,
    onPauseToggle: () -> Unit,
    onEndRound: () -> Unit
) {
    // Reads isPaused only — does not pull the timerSeconds tick into scope.
    val timer by timerStateFlow.collectAsStateWithLifecycle()
    val isPaused = timer.isPaused
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onPauseToggle,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (isPaused) stringResource(R.string.resume_timer)
                else stringResource(R.string.pause_timer)
            )
        }
        OutlinedButton(
            onClick = onEndRound,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.end_round))
        }
    }
}

@Composable
private fun GamePlayerItem(
    player: Player,
    isEliminated: Boolean
) {
    val avatarIcons = AvatarIcons

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isEliminated) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isEliminated) Color.Gray.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = avatarIcons.getOrElse(player.avatarIndex) { Icons.Filled.Person },
                    contentDescription = null,
                    tint = if (isEliminated) Color.Gray
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = player.nickname,
                style = MaterialTheme.typography.titleSmall,
                color = if (isEliminated) Color.Gray else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isEliminated) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f)
            )

            if (isEliminated) {
                Text(
                    text = stringResource(R.string.eliminated),
                    style = MaterialTheme.typography.labelSmall,
                    color = AlertRed
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun GameScreenDarkPreview() {
    CipherTheme(darkTheme = true) {
        GameScreen(onNavigateToVoting = {}, onNavigateToResult = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun GameScreenLightPreview() {
    CipherTheme(darkTheme = false) {
        GameScreen(onNavigateToVoting = {}, onNavigateToResult = {})
    }
}
