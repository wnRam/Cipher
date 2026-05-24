package uz.angrykitten.spygame.ui.howtoplay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.angrykitten.spygame.ui.theme.CipherTheme
import uz.angrykitten.spygame.ui.theme.Gold
import uz.angrykitten.spygame.ui.theme.GoldBright
import uz.angrykitten.spygame.ui.theme.GoldDeep
import uz.angrykitten.spygame.R

private data class Step(
    val number: Int,
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val SubtleScanlineColor = Color.White.copy(alpha = 0.012f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlayScreen(onNavigateBack: () -> Unit) {
    val playSteps = listOf(
        Step(1, Icons.Outlined.Shield, stringResource(R.string.play_step_1_title), stringResource(R.string.play_step_1_desc)),
        Step(2, Icons.Outlined.Forum, stringResource(R.string.play_step_2_title), stringResource(R.string.play_step_2_desc)),
        Step(3, Icons.Outlined.HowToVote, stringResource(R.string.play_step_3_title), stringResource(R.string.play_step_3_desc)),
        Step(4, Icons.Outlined.Visibility, stringResource(R.string.play_step_4_title), stringResource(R.string.play_step_4_desc)),
        Step(5, Icons.Outlined.Search, stringResource(R.string.play_step_5_title), stringResource(R.string.play_step_5_desc))
    )
    val createSteps = listOf(
        Step(1, Icons.Outlined.PersonOutline, stringResource(R.string.create_step_1_title), stringResource(R.string.create_step_1_desc)),
        Step(2, Icons.Outlined.Settings, stringResource(R.string.create_step_2_title), stringResource(R.string.create_step_2_desc)),
        Step(3, Icons.Outlined.QrCode, stringResource(R.string.create_step_3_title), stringResource(R.string.create_step_3_desc)),
        Step(4, Icons.Outlined.Group, stringResource(R.string.create_step_4_title), stringResource(R.string.create_step_4_desc)),
        Step(5, Icons.Outlined.PlayArrow, stringResource(R.string.create_step_5_title), stringResource(R.string.create_step_5_desc))
    )
    val joinSteps = listOf(
        Step(1, Icons.Outlined.PersonOutline, stringResource(R.string.join_step_1_title), stringResource(R.string.join_step_1_desc)),
        Step(2, Icons.Outlined.QrCodeScanner, stringResource(R.string.join_step_2_title), stringResource(R.string.join_step_2_desc)),
        Step(3, Icons.Outlined.MeetingRoom, stringResource(R.string.join_step_3_title), stringResource(R.string.join_step_3_desc)),
        Step(4, Icons.Outlined.PlayArrow, stringResource(R.string.join_step_4_title), stringResource(R.string.join_step_4_desc))
    )
    val offlineSteps = listOf(
        Step(1, Icons.Outlined.Settings, stringResource(R.string.offline_step_1_title), stringResource(R.string.offline_step_1_desc)),
        Step(2, Icons.Outlined.Casino, stringResource(R.string.offline_step_2_title), stringResource(R.string.offline_step_2_desc)),
        Step(3, Icons.Outlined.PhoneAndroid, stringResource(R.string.offline_step_3_title), stringResource(R.string.offline_step_3_desc)),
        Step(4, Icons.Outlined.Timer, stringResource(R.string.offline_step_4_title), stringResource(R.string.offline_step_4_desc)),
        Step(5, Icons.Outlined.RecordVoiceOver, stringResource(R.string.offline_step_5_title), stringResource(R.string.offline_step_5_desc)),
        Step(6, Icons.Outlined.Explore, stringResource(R.string.offline_step_6_title), stringResource(R.string.offline_step_6_desc))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.how_to_play),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Subtle scanline overlay — same visual language as the home
            // screen. Color cached at top-level; no per-frame allocation.
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
                                color = SubtleScanlineColor,
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                // Hero card sets the tone: pulsing gold ring + tagline.
                Spacer(Modifier.height(4.dp))
                HeroCard()
                Spacer(Modifier.height(24.dp))

                // ─── Section 1: Gameplay ───────────────────────────────
                AnimatedSection(delayMillis = 60) {
                    SectionHeader(stringResource(R.string.how_to_section_game))
                }
                Spacer(Modifier.height(16.dp))
                StaggeredSteps(playSteps, baseDelayMs = 120)

                Spacer(Modifier.height(20.dp))
                AnimatedSection(delayMillis = 100) { SectionDivider() }
                Spacer(Modifier.height(24.dp))

                // ─── Section 2: Create / Join ──────────────────────────
                AnimatedSection(delayMillis = 60) {
                    SectionHeader(stringResource(R.string.how_to_section_create_join))
                }
                Spacer(Modifier.height(16.dp))

                AnimatedSection(delayMillis = 120) {
                    SubHeader(stringResource(R.string.how_to_section_create))
                }
                Spacer(Modifier.height(10.dp))
                StaggeredSteps(createSteps, baseDelayMs = 160)

                Spacer(Modifier.height(16.dp))

                AnimatedSection(delayMillis = 120) {
                    SubHeader(stringResource(R.string.how_to_section_join))
                }
                Spacer(Modifier.height(10.dp))
                StaggeredSteps(joinSteps, baseDelayMs = 160)

                Spacer(Modifier.height(20.dp))
                AnimatedSection(delayMillis = 100) { SectionDivider() }
                Spacer(Modifier.height(24.dp))

                // ─── Section 3: Offline mode ───────────────────────────
                AnimatedSection(delayMillis = 60) {
                    SectionHeader(
                        text = stringResource(R.string.how_to_section_offline),
                        trailingIcon = Icons.Filled.WifiOff
                    )
                }
                Spacer(Modifier.height(16.dp))

                AnimatedSection(delayMillis = 120) { OfflineIntroCard() }
                Spacer(Modifier.height(14.dp))
                StaggeredSteps(offlineSteps, baseDelayMs = 160)

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ─── Hero ─────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard() {
    val transition = rememberInfiniteTransition(label = "hero")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = EaseInOutCubic),
            RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    // Cache the brush so drawBehind doesn't reallocate per recomposition.
    val brush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Gold.copy(alpha = 0.08f),
                Color.Transparent
            )
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, Gold.copy(alpha = 0.6f)), RoundedCornerShape(16.dp))
            .drawBehind {
                // Animated shimmer band slides across the hero card.
                val xOffset = (size.width * (shimmer - 0.5f) * 2f)
                drawRect(
                    brush = brush,
                    topLeft = Offset(xOffset, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height)
                )
            }
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Pulsing eye-icon disk.
            val pulse by transition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    tween(1400, easing = EaseInOutCubic),
                    RepeatMode.Reverse
                ),
                label = "pulse"
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .background(Gold.copy(alpha = 0.18f), CircleShape)
                    .border(BorderStroke(1.dp, Gold), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "CIPHER",
                    style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 6.sp),
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.how_to_hero_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Staggered step entrance ──────────────────────────────────────────────

@Composable
private fun StaggeredSteps(steps: List<Step>, baseDelayMs: Int) {
    steps.forEachIndexed { index, step ->
        AnimatedSection(delayMillis = baseDelayMs + index * 70) {
            StepCard(step)
        }
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * Mounts its content invisible, then fades + slides it up after `delayMillis`.
 * Each section / step calls this independently so the page builds itself in
 * a clear cascade rather than dropping all the dense content at once.
 */
@Composable
private fun AnimatedSection(
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(450, easing = EaseOutCubic)) +
            slideInVertically(
                animationSpec = tween(500, easing = EaseOutCubic),
                initialOffsetY = { it / 6 }
            )
    ) {
        content()
    }
}

// ─── Section headers ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    text: String,
    trailingIcon: ImageVector? = null
) {
    // Animated gold underline grows on first composition; gives each section
    // a subtle "reveal" moment instead of just appearing flat.
    val transition = rememberInfiniteTransition(label = "underline-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = EaseInOutCubic),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val barWidth = remember { mutableStateOf(0.dp) }
    LaunchedEffect(text) {
        // Quick reveal animation for the underline on each header.
        barWidth.value = 0.dp
        delay(120)
        barWidth.value = 48.dp
    }
    val animatedWidth by androidx.compose.animation.core.animateDpAsState(
        targetValue = barWidth.value,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "barWidth"
    )

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { alpha = pulse }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        // Gradient gold underline — depth without extra visual noise.
        val barBrush = remember {
            Brush.horizontalGradient(listOf(GoldBright, Gold, GoldDeep))
        }
        Box(
            modifier = Modifier
                .width(animatedWidth)
                .height(2.dp)
                .background(barBrush)
        )
    }
}

@Composable
private fun SubHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SectionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

// ─── Step card ────────────────────────────────────────────────────────────

@Composable
private fun StepCard(step: Step) {
    // Hover-ish "alive" effect: badge slowly drifts brightness; not loud
    // enough to distract, but lets the page feel responsive at idle.
    val transition = rememberInfiniteTransition(label = "step-${step.number}")
    val badgePulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2600 + step.number * 80, easing = EaseInOutCubic),
            RepeatMode.Reverse
        ),
        label = "badgePulse"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(14.dp)
            )
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(14.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Gradient gold disk for the step number.
        val numberBrush = remember {
            Brush.linearGradient(listOf(GoldBright, Gold, GoldDeep))
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer { alpha = badgePulse }
                .background(numberBrush, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.number.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        // Decorative trailing icon in a soft halo so it reads as part of the
        // card chrome rather than competing with the title.
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Gold.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = step.icon,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Offline intro card ──────────────────────────────────────────────────

@Composable
private fun OfflineIntroCard() {
    val transition = rememberInfiniteTransition(label = "offline-intro")
    val ringPulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = EaseInOutCubic),
            RepeatMode.Reverse
        ),
        label = "ringPulse"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Gold.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, Gold), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer { scaleX = ringPulse; scaleY = ringPulse }
                .background(Gold.copy(alpha = 0.18f), CircleShape)
                .border(BorderStroke(1.dp, Gold), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.WifiOff,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.offline_intro_title),
                style = MaterialTheme.typography.titleMedium,
                color = Gold,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.offline_intro_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090E)
@Composable
private fun HowToPlayScreenDarkPreview() {
    CipherTheme(darkTheme = true) { HowToPlayScreen(onNavigateBack = {}) }
}

@Preview(showBackground = true)
@Composable
private fun HowToPlayScreenLightPreview() {
    CipherTheme(darkTheme = false) { HowToPlayScreen(onNavigateBack = {}) }
}
