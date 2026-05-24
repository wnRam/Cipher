package uz.angrykitten.spygame.ui.offline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.angrykitten.spygame.R
import uz.angrykitten.spygame.ui.components.CipherPrimaryButton
import uz.angrykitten.spygame.ui.theme.Gold
import uz.angrykitten.spygame.ui.theme.MonoCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineSetupScreen(
    viewModel: OfflineModeViewModel,
    onNavigateBack: () -> Unit,
    onStart: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val packNames = listOf(
        stringResource(R.string.locations_pack),
        stringResource(R.string.food_drink_pack),
        stringResource(R.string.sports_pack),
        stringResource(R.string.movies_pack)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.offline_setup_title),
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionCard(title = stringResource(R.string.offline_players)) {
                    Text(
                        text = stringResource(R.string.offline_player_count, state.playerCount),
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items((3..10).toList()) { n ->
                            CountChip(
                                text = n.toString(),
                                selected = state.playerCount == n,
                                onClick = { viewModel.updatePlayerCount(n) }
                            )
                        }
                    }
                }

                SectionCard(title = stringResource(R.string.offline_word_pack)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(packNames) { name ->
                            val index = packNames.indexOf(name)
                            PackChip(
                                text = name,
                                selected = state.wordPackIndex == index,
                                onClick = { viewModel.updateWordPack(index) }
                            )
                        }
                    }
                }

                SectionCard(title = stringResource(R.string.offline_spies)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val maxSpies = (state.playerCount - 1).coerceAtMost(3).coerceAtLeast(1)
                        items((1..maxSpies).toList()) { n ->
                            CountChip(
                                text = n.toString(),
                                selected = state.numberOfSpies == n,
                                onClick = { viewModel.updateNumberOfSpies(n) }
                            )
                        }
                    }
                }

                SectionCard(title = stringResource(R.string.offline_timer)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.timer_duration),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .background(Gold, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.timer_minutes, state.timerMinutes),
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoCode),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Slider(
                        value = state.timerMinutes.toFloat(),
                        onValueChange = { viewModel.updateTimerMinutes(it.toInt()) },
                        valueRange = 1f..15f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = Gold,
                            activeTrackColor = Gold,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            Box(modifier = Modifier.padding(24.dp)) {
                CipherPrimaryButton(
                    text = stringResource(R.string.offline_start),
                    icon = Icons.Filled.PlayArrow,
                    onClick = {
                        viewModel.beginRound()
                        onStart()
                    }
                )
            }
        }
    }
}

@Composable
private fun CountChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) Gold else MaterialTheme.colorScheme.outline
    val bg = if (selected) Gold.copy(alpha = 0.18f) else Color.Transparent
    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 44.dp)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(6.dp))
            .background(bg, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = MonoCode,
                letterSpacing = 1.sp
            ),
            color = if (selected) Gold else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PackChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) Gold else MaterialTheme.colorScheme.outline
    val bg = if (selected) Gold.copy(alpha = 0.18f) else Color.Transparent
    Row(
        modifier = Modifier
            .border(BorderStroke(1.dp, border), RoundedCornerShape(6.dp))
            .background(bg, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Category,
            contentDescription = null,
            tint = if (selected) Gold else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Gold else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}
