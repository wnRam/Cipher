package uz.angrykitten.spygame.ui.room.lobby

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.angrykitten.spygame.R
import uz.angrykitten.spygame.model.Player
import uz.angrykitten.spygame.ui.components.CipherPrimaryButton
import uz.angrykitten.spygame.ui.theme.CipherTheme
import uz.angrykitten.spygame.ui.theme.DangerRed
import uz.angrykitten.spygame.ui.theme.Gold
import uz.angrykitten.spygame.ui.theme.MonoCode
import uz.angrykitten.spygame.ui.components.AvatarIcons

private val avatarIcons = AvatarIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    isHost: Boolean,
    onNavigateBack: () -> Unit,
    onGameStart: () -> Unit,
    viewModel: LobbyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Lifecycle-aware QR bitmap subscription so the screen does not hold
    // onto the bitmap while backgrounded.
    val qrBitmap by viewModel.qrBitmap.collectAsStateWithLifecycle()
    var kickTarget by remember { mutableStateOf<Player?>(null) }
    val context = LocalContext.current

    LaunchedEffect(isHost) { viewModel.bind(isHost) }
    LaunchedEffect(Unit) {
        viewModel.gameStarted.collect { if (it) onGameStart() }
    }
    LaunchedEffect(Unit) {
        viewModel.navigateHome.collect { if (it) onNavigateBack() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHost) stringResource(R.string.room_code)
                        else stringResource(R.string.waiting_for_host),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.leaveRoom()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.error == "session_missing") {
                Spacer(Modifier.height(40.dp))
                Text(
                    text = stringResource(R.string.room_create_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DangerRed
                )
                Spacer(Modifier.height(16.dp))
                CipherPrimaryButton(
                    text = stringResource(R.string.retry),
                    onClick = onNavigateBack
                )
                return@Scaffold
            }

            if (!uiState.ready) {
                Spacer(Modifier.height(40.dp))
                CircularProgressIndicator(color = Gold)
                return@Scaffold
            }

            if (isHost) {
                QrFrame(qrBitmap = qrBitmap)
                Spacer(Modifier.height(16.dp))
                RoomCodeDisplay(
                    code = uiState.roomCode,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Cipher Room Code", uiState.roomCode))
                    }
                )
                Spacer(Modifier.height(8.dp))
                WaitingDots()
                Spacer(Modifier.height(24.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.players_joined, uiState.players.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (isHost && uiState.players.size < 3) {
                    Text(
                        text = stringResource(R.string.min_players_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRed.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.players, key = { it.id }) { player ->
                    PlayerRow(
                        player = player,
                        canKick = isHost && !player.isHost,
                        onKick = { kickTarget = player }
                    )
                }
            }

            if (isHost) {
                Spacer(Modifier.height(12.dp))
                StartGameButton(
                    enabled = uiState.canStart,
                    onClick = { viewModel.startGame() }
                )
            }
        }
    }

    kickTarget?.let { player ->
        AlertDialog(
            onDismissRequest = { kickTarget = null },
            title = { Text(stringResource(R.string.kick_player)) },
            text = { Text(stringResource(R.string.kick_confirm, player.nickname)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.kickPlayer(player.id)
                        kickTarget = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                ) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { kickTarget = null }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }

}

@Composable
private fun QrFrame(qrBitmap: ImageBitmap?) {
    // The ImageBitmap is generated off-thread by the ViewModel and cached
    // for the host session, so this composable never blocks the main thread.
    Box(
        modifier = Modifier
            .size(232.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, Gold), RoundedCornerShape(16.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(8.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = stringResource(R.string.scan_to_join),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(color = Gold)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.scan_to_join),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RoomCodeDisplay(code: String, onCopy: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = code.toCharArray().joinToString("  "),
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = MonoCode,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = 4.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = stringResource(R.string.copy_code),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun WaitingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val dotCount = phase.toInt().coerceIn(0, 3)
    val dots = ".".repeat(dotCount)
    Text(
        text = stringResource(R.string.waiting_for_players) + " $dots",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PlayerRow(player: Player, canKick: Boolean, onKick: () -> Unit) {
    val baseModifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))

    val clickableModifier = if (canKick) baseModifier.clickable { onKick() } else baseModifier

    Row(
        modifier = clickableModifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Gold.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = avatarIcons.getOrElse(player.avatarIndex) { Icons.Filled.Person },
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = player.nickname,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (player.isHost) {
            HostBadge()
        }
    }
}

@Composable
private fun HostBadge() {
    Box(
        modifier = Modifier
            .background(Gold, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.host_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StartGameButton(enabled: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = EaseInOutCubic),
            RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier.alpha(if (enabled) pulseAlpha else 0.5f)
    ) {
        CipherPrimaryButton(
            text = stringResource(R.string.start_game),
            icon = Icons.Filled.PlayArrow,
            enabled = enabled,
            onClick = onClick
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090E)
@Composable
private fun LobbyDarkPreview() {
    CipherTheme(darkTheme = true) {
        LobbyScreen(isHost = true, onNavigateBack = {}, onGameStart = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LobbyLightPreview() {
    CipherTheme(darkTheme = false) {
        LobbyScreen(isHost = true, onNavigateBack = {}, onGameStart = {})
    }
}
