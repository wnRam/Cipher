package uz.angrykitten.spygame.ui.room.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.angrykitten.spygame.R
import uz.angrykitten.spygame.model.Role
import uz.angrykitten.spygame.ui.components.CipherPrimaryButton
import uz.angrykitten.spygame.ui.theme.CipherTheme
import uz.angrykitten.spygame.ui.theme.DangerRed
import uz.angrykitten.spygame.ui.theme.Gold
import uz.angrykitten.spygame.ui.theme.MonoCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScreen(
    onNavigateBack: () -> Unit,
    onRoomCreated: () -> Unit,
    viewModel: CreateRoomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.roomCreated.collect { success -> if (success) onRoomCreated() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.create_room),
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
            UnderlineTabRow(
                tabs = listOf(
                    stringResource(R.string.game_rules),
                    stringResource(R.string.words_roles)
                ),
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> GameRulesTab(uiState, viewModel)
                    1 -> WordsRolesTab(uiState, viewModel)
                }
            }

            uiState.errorResId?.let { resId ->
                Text(
                    text = stringResource(resId),
                    color = DangerRed,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }

            Box(modifier = Modifier.padding(24.dp)) {
                CipherPrimaryButton(
                    text = stringResource(R.string.create_game),
                    icon = Icons.Filled.RocketLaunch,
                    loading = uiState.isCreating,
                    enabled = !uiState.isCreating,
                    onClick = { viewModel.createRoom() }
                )
            }
        }
    }
}

@Composable
private fun UnderlineTabRow(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        tabs.forEachIndexed { index, label ->
            val active = index == selected
            val color = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = color
                )
                Spacer(Modifier.height(8.dp))
                val underline = animateDpAsState(
                    targetValue = if (active) 32.dp else 0.dp,
                    animationSpec = tween(220),
                    label = "tabUnderline"
                ).value
                Box(
                    modifier = Modifier
                        .width(underline)
                        .height(2.dp)
                        .background(Gold)
                )
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun GameRulesTab(state: CreateRoomUiState, viewModel: CreateRoomViewModel) {
    SectionCard(title = stringResource(R.string.timer_duration)) {
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
            GoldBadge(text = stringResource(R.string.timer_minutes, state.timerMinutes))
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

    SectionCard(title = stringResource(R.string.win_conditions)) {
        GoldCheckRow(
            checked = state.spyWinsByHiding,
            label = stringResource(R.string.spy_wins_hiding),
            onCheckedChange = viewModel::updateSpyWinsByHiding
        )
        Spacer(Modifier.height(8.dp))
        GoldCheckRow(
            checked = state.spyWinsByGuessing,
            label = stringResource(R.string.spy_wins_guessing),
            onCheckedChange = viewModel::updateSpyWinsByGuessing
        )
    }

    SectionCard(title = stringResource(R.string.number_of_spies)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.updateNumberOfSpies(state.numberOfSpies - 1) },
                enabled = state.numberOfSpies > 1
            ) {
                Icon(Icons.Filled.Remove, contentDescription = null, tint = Gold)
            }
            Text(
                text = state.numberOfSpies.toString(),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = MonoCode,
                    letterSpacing = 2.sp
                ),
                color = Gold
            )
            IconButton(
                onClick = { viewModel.updateNumberOfSpies(state.numberOfSpies + 1) },
                enabled = state.numberOfSpies < 3
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Gold)
            }
        }
    }
}

@Composable
private fun WordsRolesTab(state: CreateRoomUiState, viewModel: CreateRoomViewModel) {
    SectionCard(title = stringResource(R.string.word_pack)) {
        val packNames = listOf(
            stringResource(R.string.locations_pack),
            stringResource(R.string.food_drink_pack),
            stringResource(R.string.sports_pack),
            stringResource(R.string.movies_pack),
            stringResource(R.string.custom_pack)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(packNames) { name ->
                val index = packNames.indexOf(name)
                val selected = state.selectedPackIndex == index
                PackChip(
                    text = name,
                    selected = selected,
                    onClick = { viewModel.updateSelectedPack(index) }
                )
            }
        }

        AnimatedVisibility(
            visible = state.selectedPackIndex >= viewModel.wordPacks.size,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180))
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.customWords,
                    onValueChange = { viewModel.updateCustomWords(it) },
                    label = { Text(stringResource(R.string.custom_words)) },
                    placeholder = { Text(stringResource(R.string.custom_words_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 3
                )
            }
        }
    }

    SectionCard(title = stringResource(R.string.custom_roles)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = state.customRolesEnabled,
                onCheckedChange = { viewModel.toggleCustomRoles() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = Gold,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.custom_roles),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        AnimatedVisibility(state.customRolesEnabled, enter = fadeIn(), exit = fadeOut()) {
            Column {
                Spacer(Modifier.height(8.dp))
                state.roles.forEachIndexed { index, role ->
                    RoleEditor(
                        role = role,
                        onUpdate = { viewModel.updateRole(index, it) },
                        onRemove = { viewModel.removeRole(index) },
                        canRemove = state.roles.size > 2
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier.clickable { viewModel.addRole() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.add_role),
                        style = MaterialTheme.typography.labelLarge,
                        color = Gold
                    )
                }
            }
        }
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
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Gold else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoldCheckRow(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(
                    BorderStroke(1.dp, Gold),
                    RoundedCornerShape(4.dp)
                )
                .background(
                    if (checked) Gold else Color.Transparent,
                    RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GoldBadge(text: String) {
    Box(
        modifier = Modifier
            .background(Gold, CircleShape)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoCode),
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RoleEditor(
    role: Role,
    onUpdate: (Role) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = role.name,
                onValueChange = { onUpdate(role.copy(name = it)) },
                label = { Text(stringResource(R.string.role_name)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.remove_role),
                        tint = DangerRed
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        GoldCheckRow(
            checked = role.isSpy,
            label = stringResource(R.string.is_spy),
            onCheckedChange = { onUpdate(role.copy(isSpy = it)) }
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

@Preview(showBackground = true, backgroundColor = 0xFF08090E)
@Composable
private fun CreateRoomDarkPreview() {
    CipherTheme(darkTheme = true) {
        CreateRoomScreen(onNavigateBack = {}, onRoomCreated = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateRoomLightPreview() {
    CipherTheme(darkTheme = false) {
        CreateRoomScreen(onNavigateBack = {}, onRoomCreated = {})
    }
}
