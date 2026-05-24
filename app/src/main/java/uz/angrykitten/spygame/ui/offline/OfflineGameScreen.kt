package uz.angrykitten.spygame.ui.offline

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.angrykitten.spygame.R
import uz.angrykitten.spygame.ui.components.CipherPrimaryButton
import uz.angrykitten.spygame.ui.theme.AlertRed
import uz.angrykitten.spygame.ui.theme.Gold
import uz.angrykitten.spygame.ui.theme.MonoCode
import uz.angrykitten.spygame.ui.theme.NeonGreen
import uz.angrykitten.spygame.ui.theme.WarningAmber

@Composable
fun OfflineGameScreen(
    viewModel: OfflineModeViewModel,
    onRoundEnded: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startTimer()
    }

    // Back-press during a live round shouldn't silently drop the game.
    // Show a confirmation dialog — accidental back is the more common case.
    BackHandler(enabled = !showExitConfirm) {
        // Pause the timer the moment the user thinks about leaving so the
        // round doesn't keep draining while they decide.
        if (!state.isPaused) viewModel.togglePause()
        showExitConfirm = true
    }

    // Pause when backgrounded so the round doesn't drain unobserved.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAppBackgrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.offline_exit_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.offline_exit_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirm = false
                        viewModel.endRound()
                        onRoundEnded()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AlertRed)
                ) { Text(stringResource(R.string.offline_exit_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    // Resume on cancel — we paused on back-press above.
                    if (state.isPaused) viewModel.togglePause()
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Offline mode doesn't track multi-round scoring on screen, but
            // we still show "Round 1" for parity with the multiplayer header.
            Text(
                text = stringResource(R.string.offline_round_n, 1),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            TimerDisplay(
                seconds = state.timerSeconds,
                totalSeconds = state.totalSeconds,
                isPaused = state.isPaused
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.offline_mark_eliminated),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.playerCount, key = { it }) { slot ->
                    val isEliminated = slot in state.eliminated
                    PlayerSlotRow(
                        slot = slot,
                        isEliminated = isEliminated,
                        onClick = { viewModel.toggleEliminated(slot) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.togglePause() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (state.isPaused) stringResource(R.string.resume_timer)
                        else stringResource(R.string.pause_timer)
                    )
                }
                OutlinedButton(
                    onClick = {
                        viewModel.endRound()
                        onRoundEnded()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AlertRed
                    )
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.offline_end_round))
                }
            }

            Spacer(Modifier.height(10.dp))

            CipherPrimaryButton(
                text = stringResource(R.string.offline_reveal_location),
                icon = Icons.Filled.Place,
                onClick = {
                    viewModel.revealLocation()
                    onRoundEnded()
                }
            )
        }
    }
}

@Composable
private fun TimerDisplay(seconds: Int, totalSeconds: Int, isPaused: Boolean) {
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
        label = "offlineTimerColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(180.dp)
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    style = Stroke(width = 12f)
                )
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
                    letterSpacing = 2.sp,
                    fontFamily = MonoCode
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
private fun PlayerSlotRow(slot: Int, isEliminated: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isEliminated)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                else Gold.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (slot + 1).toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoCode),
                        color = if (isEliminated) Color.Gray else Gold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.offline_player_n, slot + 1),
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
