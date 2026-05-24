package uz.angrykitten.spygame.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.angrykitten.spygame.R
import uz.angrykitten.spygame.ui.components.CipherPrimaryButton
import uz.angrykitten.spygame.ui.components.CipherSecondaryButton
import uz.angrykitten.spygame.ui.settings.SettingsViewModel
import uz.angrykitten.spygame.ui.theme.CipherTheme
import uz.angrykitten.spygame.ui.theme.DangerRed
import uz.angrykitten.spygame.ui.theme.Gold

@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToCreateRoom: () -> Unit,
    onNavigateToJoinRoom: () -> Unit,
    onNavigateToOfflineMode: () -> Unit,
    onNavigateToHowToPlay: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val isWifiConnected by viewModel.isWifiConnected.collectAsStateWithLifecycle()
    val language by settingsViewModel.language.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScanlineOverlay()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: language pill | settings gear (single entry point)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguagePill(
                    current = language,
                    onSelect = settingsViewModel::setLanguage
                )
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            CipherWordmark()

            Spacer(Modifier.height(64.dp))

            AnimatedVisibility(
                visible = !isWifiConnected,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(180))
            ) {
                WifiBanner()
            }

            if (!isWifiConnected) Spacer(Modifier.height(16.dp))

            CipherPrimaryButton(
                text = stringResource(R.string.create_room),
                icon = Icons.Filled.AddCircleOutline,
                onClick = onNavigateToCreateRoom
            )

            Spacer(Modifier.height(12.dp))

            CipherPrimaryButton(
                text = stringResource(R.string.join_room),
                icon = Icons.Outlined.Login,
                onClick = onNavigateToJoinRoom
            )

            Spacer(Modifier.height(12.dp))

            CipherSecondaryButton(
                text = stringResource(R.string.offline_mode),
                icon = Icons.Filled.WifiOff,
                onClick = onNavigateToOfflineMode,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CipherSecondaryButton(
                    text = stringResource(R.string.how_to_play),
                    icon = Icons.Outlined.HelpOutline,
                    onClick = onNavigateToHowToPlay,
                    modifier = Modifier.weight(1f)
                )
                CipherSecondaryButton(
                    text = stringResource(R.string.profile),
                    icon = Icons.Outlined.Person,
                    onClick = onNavigateToProfile,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CipherWordmark() {
    val infiniteTransition = rememberInfiniteTransition(label = "underline")
    val underlineWidth by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "underlineWidth"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "CIPHER",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 64.sp,
                letterSpacing = 12.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(underlineWidth.coerceIn(0.5f, 1f) * 0.55f)
                .height(1.dp)
                .background(Gold)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.cipher_subtitle),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LanguagePill(
    current: String,
    onSelect: (String) -> Unit
) {
    val languages = listOf("en", "ru", "uz")
    Row(
        modifier = Modifier
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(20.dp)
            )
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        languages.forEach { lang ->
            val selected = lang == current
            Box(
                modifier = Modifier
                    .clickable { onSelect(lang) }
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = lang.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WifiBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                RoundedCornerShape(12.dp)
            )
            .background(DangerRed.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.WifiOff,
            contentDescription = null,
            tint = DangerRed,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(R.string.no_wifi),
            style = MaterialTheme.typography.bodyMedium,
            color = DangerRed
        )
    }
}

private val ScanlineColor = Color.White.copy(alpha = 0.015f)

@Composable
private fun ScanlineOverlay() {
    // `drawBehind` runs every frame of the parent's invalidation; keeping the
    // color constant (top-level val) avoids reallocating per draw, and the
    // tight loop is the only work inside the closure.
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
                        color = ScanlineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += step
                    i++
                }
            }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF08090E)
@Composable
private fun HomeScreenDarkPreview() {
    CipherTheme(darkTheme = true) {
        HomeScreen({}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenLightPreview() {
    CipherTheme(darkTheme = false) {
        HomeScreen({}, {}, {}, {}, {}, {})
    }
}
