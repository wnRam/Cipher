package uz.angrykitten.spygame.ui.game.reveal

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import uz.angrykitten.spygame.R
import uz.angrykitten.spygame.model.Player
import uz.angrykitten.spygame.model.RevealMode
import uz.angrykitten.spygame.model.Role
import uz.angrykitten.spygame.ui.components.CipherPrimaryButton
import uz.angrykitten.spygame.ui.theme.DangerRed
import uz.angrykitten.spygame.ui.theme.Gold
import uz.angrykitten.spygame.ui.theme.MonoCode
import uz.angrykitten.spygame.ui.theme.SuccessGreen
import uz.angrykitten.spygame.ui.components.AvatarIcons

private val avatarIcons = AvatarIcons

@Composable
fun RoleRevealScreen(
    onNavigateToGame: () -> Unit,
    viewModel: RoleRevealViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // No back navigation during the entire reveal sequence.
    BackHandler(enabled = true) { /* swallow */ }

    // Lifecycle: spec says if app is backgrounded during VIEWING, drop back
    // to HAND_OFF.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAppBackgroundedDuringViewing()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToGame.collect { if (it) onNavigateToGame() }
    }

    // FLAG_SECURE + FLAG_KEEP_SCREEN_ON during VIEWING only.
    val view = LocalView.current
    DisposableEffect(uiState.revealPhase, uiState.revealMode) {
        val window = (view.context as? Activity)?.window
        if (uiState.revealMode == RevealMode.PASS_THE_PHONE &&
            uiState.revealPhase == RevealPhase.VIEWING
        ) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            // Peer device in pass-the-phone mode: waiting screen.
            uiState.revealMode == RevealMode.PASS_THE_PHONE && !uiState.isHost ->
                PeerWaitingScreen(
                    confirmed = uiState.progressConfirmed,
                    total = uiState.progressTotal
                )

            // Host, pass-the-phone state machine.
            uiState.revealMode == RevealMode.PASS_THE_PHONE && uiState.isHost ->
                when (uiState.revealPhase) {
                    RevealPhase.HAND_OFF -> HandOffPhase(
                        round = uiState.roundNumber,
                        player = uiState.orderedPlayers.getOrNull(uiState.currentRevealIndex),
                        currentIsSelf = uiState.currentIsSelf,
                        index = uiState.currentRevealIndex,
                        total = uiState.orderedPlayers.size,
                        onReady = viewModel::onPlayerReady
                    )
                    RevealPhase.HIDDEN -> HiddenPhase(
                        onComplete = viewModel::onHiddenComplete
                    )
                    RevealPhase.VIEWING -> ViewingPhase(
                        role = uiState.currentRole,
                        location = uiState.currentLocation,
                        onConfirm = viewModel::onRoleConfirmed
                    )
                    RevealPhase.CONFIRMED -> ConfirmedPhase(
                        playerName = uiState.orderedPlayers
                            .getOrNull(uiState.currentRevealIndex)?.nickname.orEmpty(),
                        isLast = uiState.currentRevealIndex + 1 >= uiState.orderedPlayers.size,
                        onComplete = viewModel::onConfirmedComplete
                    )
                    RevealPhase.ALL_DONE -> AllDonePhase(
                        players = uiState.orderedPlayers,
                        onBegin = viewModel::onBeginMission
                    )
                }

            // OWN_DEVICE flow (host + peer).
            else -> OwnDeviceFlow(
                isRevealed = uiState.isRevealed,
                isReady = uiState.isReady,
                role = uiState.myRole,
                location = uiState.myLocation,
                readyCount = uiState.readyCount,
                totalCount = uiState.totalCount,
                isHost = uiState.isHost,
                allReady = uiState.allReady,
                onReveal = viewModel::revealRole,
                onConfirm = viewModel::confirmSeen,
                onStart = viewModel::onBeginMission
            )
        }
    }
}

// ─── Pass-the-phone phase screens ──────────────────────────────────────────

@Composable
private fun HandOffPhase(
    round: Int,
    player: Player?,
    currentIsSelf: Boolean,
    index: Int,
    total: Int,
    onReady: () -> Unit
) {
    if (player == null) return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.reveal_header, round),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoCode),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Gold.copy(alpha = 0.15f), CircleShape)
                .border(BorderStroke(2.dp, Gold), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = avatarIcons.getOrElse(player.avatarIndex) { Icons.Filled.Person },
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = player.nickname,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.hand_off_to, player.nickname),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // Warning card with eye-slash icon.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Gold), RoundedCornerShape(12.dp))
                .background(Gold.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.VisibilityOff,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (currentIsSelf)
                    stringResource(R.string.hand_off_self)
                else
                    stringResource(R.string.hand_off_warning, player.nickname),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(32.dp))

        CipherPrimaryButton(
            text = stringResource(R.string.im_alone_ready),
            icon = Icons.Filled.Check,
            onClick = onReady
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.player_progress, index + 1, total),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoCode),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HiddenPhase(onComplete: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        // Dim the screen during the blackout to discourage glances.
        val originalBrightness = window?.attributes?.screenBrightness
        window?.let {
            val attrs = it.attributes
            attrs.screenBrightness = 0.01f
            it.attributes = attrs
        }
        onDispose {
            window?.let {
                val attrs = it.attributes
                attrs.screenBrightness = originalBrightness
                    ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                it.attributes = attrs
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(1500)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Gold, strokeWidth = 2.dp)
    }
}

@Composable
private fun ViewingPhase(
    role: Role?,
    location: String?,
    onConfirm: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(3) }
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }
    val canConfirm = countdown <= 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CIPHER",
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 6.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.your_role_label),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoCode),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        RoleCard(role = role)

        Spacer(Modifier.height(20.dp))

        LocationBlock(isSpy = role?.isSpy == true, location = location)

        Spacer(Modifier.height(24.dp))

        CipherPrimaryButton(
            text = if (canConfirm) stringResource(R.string.seen_my_role)
            else stringResource(R.string.seen_my_role_countdown, countdown),
            icon = if (canConfirm) Icons.Filled.Check else null,
            enabled = canConfirm,
            onClick = onConfirm
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.do_not_show),
            style = MaterialTheme.typography.labelSmall,
            color = DangerRed.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RoleCard(role: Role?) {
    val isSpy = role?.isSpy == true
    val icon: ImageVector = if (isSpy) Icons.Filled.VisibilityOff else Icons.Filled.Person
    val tint = if (isSpy) DangerRed else Gold

    Box(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .height(280.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .border(BorderStroke(2.dp, Gold), RoundedCornerShape(16.dp))
            .graphicsLayer { alpha = 1f },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = role?.name?.uppercase() ?: "",
                style = MaterialTheme.typography.headlineLarge,
                color = tint,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = role?.description.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LocationBlock(isSpy: Boolean, location: String?) {
    val bg = if (isSpy) DangerRed.copy(alpha = 0.10f) else Gold.copy(alpha = 0.08f)
    val border = if (isSpy) DangerRed else Gold
    val icon = if (isSpy) Icons.AutoMirrored.Filled.HelpOutline else Icons.Filled.LocationOn

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, border), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = border,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(
                    if (isSpy) R.string.your_mission else R.string.secret_location
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isSpy) stringResource(R.string.spy_mission_text)
                else (location ?: "").uppercase(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = if (isSpy) null else MonoCode
                ),
                color = border,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isSpy) stringResource(R.string.spy_mission_subtext)
                else stringResource(R.string.remember_location),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isSpy) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.hidden_question_marks),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = MonoCode,
                        letterSpacing = 4.sp
                    ),
                    color = DangerRed.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ConfirmedPhase(
    playerName: String,
    isLast: Boolean,
    onComplete: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onComplete()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(Gold.copy(alpha = 0.18f), CircleShape)
                .border(BorderStroke(2.dp, Gold), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.player_is_ready, playerName),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isLast) stringResource(R.string.all_players_ready)
            else stringResource(R.string.pass_phone_to_next),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AllDonePhase(players: List<Player>, onBegin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CIPHER",
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 6.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Gold.copy(alpha = 0.15f), CircleShape)
                .border(BorderStroke(2.dp, Gold), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.all_agents_briefed),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.all_briefed_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // Player summary list (names + checkmarks only — never roles).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    RoundedCornerShape(12.dp)
                )
                .padding(vertical = 8.dp)
        ) {
            players.forEach { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Gold.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = avatarIcons.getOrElse(p.avatarIndex) { Icons.Filled.Person },
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = p.nickname,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        CipherPrimaryButton(
            text = stringResource(R.string.begin_mission),
            icon = Icons.Filled.PlayArrow,
            onClick = onBegin
        )
    }
}

// ─── Peer-side waiting screen ──────────────────────────────────────────────

@Composable
private fun PeerWaitingScreen(confirmed: Int, total: Int) {
    val infinite = rememberInfiniteTransition(label = "lock-pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = EaseInOutCubic),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.VisibilityOff,
            contentDescription = null,
            tint = Gold,
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer { alpha = pulse }
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.waiting_for_roles),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.waiting_for_roles_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (total > 0) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.reveal_progress_count, confirmed, total),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoCode),
                color = Gold
            )
        }
    }
}

// ─── OWN_DEVICE flow ───────────────────────────────────────────────────────

@Composable
private fun OwnDeviceFlow(
    isRevealed: Boolean,
    isReady: Boolean,
    role: Role?,
    location: String?,
    readyCount: Int,
    totalCount: Int,
    isHost: Boolean,
    allReady: Boolean,
    onReveal: () -> Unit,
    onConfirm: () -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = isRevealed,
            transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 })
                    .togetherWith(fadeOut(tween(200)))
            },
            label = "revealCard"
        ) { revealed ->
            if (!revealed) {
                ClassifiedCard(onClick = onReveal)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RoleCard(role = role)
                    Spacer(Modifier.height(16.dp))
                    LocationBlock(isSpy = role?.isSpy == true, location = location)
                    Spacer(Modifier.height(20.dp))
                    if (!isReady) {
                        CipherPrimaryButton(
                            text = stringResource(R.string.seen_my_role),
                            icon = Icons.Filled.Check,
                            onClick = onConfirm
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .background(
                                    SuccessGreen.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, SuccessGreen),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.seen_my_role),
                                style = MaterialTheme.typography.labelLarge,
                                color = SuccessGreen
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        if (totalCount > 0) {
            Text(
                text = stringResource(R.string.ready_count, readyCount, totalCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isHost && allReady) {
            Spacer(Modifier.height(16.dp))
            CipherPrimaryButton(
                text = stringResource(R.string.start_game),
                icon = Icons.Filled.PlayArrow,
                onClick = onStart
            )
        }
    }
}

@Composable
private fun ClassifiedCard(onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = EaseInOutCubic),
            RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(360.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, Gold), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.VisibilityOff,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer { alpha = pulseAlpha }
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.classified),
                style = MaterialTheme.typography.displaySmall.copy(letterSpacing = 6.sp),
                color = Gold,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.tap_to_reveal),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = pulseAlpha)
            )
        }
    }
}
