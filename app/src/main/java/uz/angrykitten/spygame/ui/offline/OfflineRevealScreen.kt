package uz.angrykitten.spygame.ui.offline

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.angrykitten.spygame.R
import uz.angrykitten.spygame.model.Role
import uz.angrykitten.spygame.ui.components.CipherPrimaryButton
import uz.angrykitten.spygame.ui.theme.DangerRed
import uz.angrykitten.spygame.ui.theme.Gold
import uz.angrykitten.spygame.ui.theme.MonoCode

@Composable
fun OfflineRevealScreen(
    viewModel: OfflineModeViewModel,
    onAllRevealed: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = true) { /* swallow during reveal */ }

    // FLAG_SECURE during VIEWING to discourage screenshots/peeks.
    val view = LocalView.current
    DisposableEffect(state.revealPhase) {
        val window = (view.context as? Activity)?.window
        if (state.revealPhase == OfflineRevealPhase.VIEWING) {
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
            // Edge-to-edge: respect status bar so the player counter isn't
            // sliced by the notch/cutout.
            .systemBarsPadding()
    ) {
        when {
            state.allRevealed -> AllDoneScreen(
                playerCount = state.playerCount,
                onBegin = onAllRevealed
            )
            else -> RevealPhaseScreen(
                index = state.currentRevealIndex,
                total = state.playerCount,
                phase = state.revealPhase,
                role = viewModel.currentRole(),
                location = viewModel.currentLocationForPlayer(),
                isLast = state.currentRevealIndex + 1 >= state.playerCount,
                onTapReveal = viewModel::onTapReveal,
                onDone = viewModel::onDone,
                onNext = viewModel::onNext
            )
        }
    }
}

@Composable
private fun RevealPhaseScreen(
    index: Int,
    total: Int,
    phase: OfflineRevealPhase,
    role: Role?,
    location: String?,
    isLast: Boolean,
    onTapReveal: () -> Unit,
    onDone: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.offline_reveal_player, index + 1, total),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoCode),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 })
                    .togetherWith(fadeOut(tween(200)))
            },
            label = "offlineRevealPhase"
        ) { current ->
            when (current) {
                OfflineRevealPhase.WAITING -> WaitingCard(
                    playerNumber = index + 1,
                    onTap = onTapReveal
                )
                OfflineRevealPhase.VIEWING -> ViewingBlock(
                    role = role,
                    location = location,
                    onDone = onDone
                )
                OfflineRevealPhase.DONE -> DoneBlock(
                    isLast = isLast,
                    onNext = onNext
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WaitingCard(playerNumber: Int, onTap: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "wait-pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = EaseInOutCubic),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )
    // Subtle border glow draws attention without being noisy.
    val borderAlpha by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = EaseInOutCubic),
            RepeatMode.Reverse
        ),
        label = "borderGlow"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.offline_pass_to, playerNumber),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(340.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                .border(
                    BorderStroke(1.dp, Gold.copy(alpha = borderAlpha)),
                    RoundedCornerShape(20.dp)
                )
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.VisibilityOff,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer { alpha = pulse }
                )
                Spacer(Modifier.height(16.dp))
                // Tightened letterSpacing + headlineLarge so the word fits
                // on one line even on narrow screens; maxLines=1 + softWrap
                // false guarantees no awkward mid-word break.
                Text(
                    text = stringResource(R.string.classified),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        letterSpacing = 4.sp,
                        fontSize = 28.sp
                    ),
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.offline_tap_to_reveal_role),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = pulse)
                )
            }
        }
    }
}

@Composable
private fun ViewingBlock(role: Role?, location: String?, onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RoleCard(role = role)
        Spacer(Modifier.height(16.dp))
        LocationBlock(isSpy = role?.isSpy == true, location = location)
        Spacer(Modifier.height(20.dp))
        CipherPrimaryButton(
            text = stringResource(R.string.offline_seen_done),
            icon = Icons.Filled.Check,
            onClick = onDone
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
private fun DoneBlock(isLast: Boolean, onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            text = if (isLast) stringResource(R.string.offline_all_seen)
            else stringResource(R.string.pass_phone_to_next),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        CipherPrimaryButton(
            text = if (isLast) stringResource(R.string.offline_start_round)
            else stringResource(R.string.offline_next_player),
            icon = if (isLast) Icons.Filled.PlayArrow else Icons.Filled.ArrowForward,
            onClick = onNext
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
            .border(BorderStroke(2.dp, Gold), RoundedCornerShape(16.dp)),
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
        }
    }
}

@Composable
private fun AllDoneScreen(playerCount: Int, onBegin: () -> Unit) {
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
            text = stringResource(R.string.offline_all_seen),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.offline_player_count, playerCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        CipherPrimaryButton(
            text = stringResource(R.string.offline_start_round),
            icon = Icons.Filled.PlayArrow,
            onClick = onBegin
        )
    }
}
