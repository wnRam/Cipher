package uz.angrykitten.spygame.ui.game.result

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.angrykitten.spygame.R
import uz.angrykitten.spygame.model.WinningSide
import uz.angrykitten.spygame.ui.theme.*

private val ResultScanlineColor = Color.White.copy(alpha = 0.015f)

@Composable
fun RoundResultScreen(
    onNextRound: () -> Unit,
    onBackToLobby: () -> Unit,
    onEndGame: () -> Unit,
    viewModel: RoundResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isSpyWin = uiState.winningSide == WinningSide.SPY
    val accentColor = if (isSpyWin) AlertRed else NeonGreen

    val infiniteTransition = rememberInfiniteTransition(label = "result")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Scanline overlay — color is cached at top-level so drawBehind
        // does not reallocate a Color instance on every frame.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val step = 4f
                    val lineCount = (size.height / step).toInt()
                    var y = 0f
                    var i = 0
                    while (i <= lineCount) {
                        drawLine(
                            color = ResultScanlineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += step
                        i++
                    }
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            // Win announcement
            Icon(
                imageVector = if (isSpyWin) Icons.Filled.Visibility else Icons.Filled.Shield,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isSpyWin) stringResource(R.string.spy_wins)
                else stringResource(R.string.players_win),
                style = MaterialTheme.typography.displaySmall.copy(
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Black
                ),
                color = accentColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Spy reveal
            if (uiState.spyNames.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AlertRed.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.spy_was),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        uiState.spyNames.forEach { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = AlertRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location reveal
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.the_location_was),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.location,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Spy's last chance
            if (uiState.showSpyGuess && uiState.spyGuessResult == null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.spy_last_chance),
                    style = MaterialTheme.typography.titleLarge,
                    color = WarningAmber
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.guess_location),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.spyGuessOptions.forEach { option ->
                        OutlinedButton(
                            onClick = { viewModel.submitSpyGuess(option) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(option)
                        }
                    }
                }
            }

            // Spy guess result
            uiState.spyGuessResult?.let { correct ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (correct) stringResource(R.string.correct_guess)
                    else stringResource(R.string.wrong_guess),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (correct) NeonGreen else AlertRed,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons (host only)
            if (uiState.isHost) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onNextRound,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.next_round))
                    }
                    OutlinedButton(
                        onClick = onBackToLobby,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.back_to_lobby))
                    }
                    TextButton(
                        onClick = onEndGame,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.end_game),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun RoundResultScreenDarkPreview() {
    CipherTheme(darkTheme = true) {
        RoundResultScreen(onNextRound = {}, onBackToLobby = {}, onEndGame = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun RoundResultScreenLightPreview() {
    CipherTheme(darkTheme = false) {
        RoundResultScreen(onNextRound = {}, onBackToLobby = {}, onEndGame = {})
    }
}
