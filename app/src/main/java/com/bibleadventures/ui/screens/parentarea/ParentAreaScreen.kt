package com.bibleadventures.ui.screens.parentarea

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bibleadventures.R
import com.bibleadventures.ui.AppViewModelProvider
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlin.random.Random

@Composable
fun ParentAreaScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ParentAreaViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ParentAreaContent(
        uiState = uiState,
        onBack = onBack,
        onOpenSettings = onOpenSettings,
        onResetConfirmed = viewModel::onResetProgressConfirmed,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentAreaContent(
    uiState: ParentAreaUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onResetConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Intentionally resets every time this screen is (re-)entered — a
    // parental gate that "stays unlocked" across visits defeats its own
    // purpose.
    var gateUnlocked by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.parent_area_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (gateUnlocked) {
            ParentAreaSummary(
                uiState = uiState,
                onOpenSettings = onOpenSettings,
                onResetConfirmed = onResetConfirmed,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            ParentalGate(
                onUnlocked = { gateUnlocked = true },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

/** A simple math challenge — appropriate for this app's target age, no PIN to set up or store. */
@Composable
private fun ParentalGate(onUnlocked: () -> Unit, modifier: Modifier = Modifier) {
    var operandA by remember { mutableStateOf(Random.nextInt(2, 10)) }
    var operandB by remember { mutableStateOf(Random.nextInt(2, 10)) }
    var answerText by remember { mutableStateOf("") }
    var showWrongAnswer by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = stringResource(R.string.parent_area_gate_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = stringResource(R.string.parent_area_gate_instructions),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.parent_area_gate_question, operandA, operandB),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp).testTag("parent_gate_question"),
        )
        OutlinedTextField(
            value = answerText,
            onValueChange = { answerText = it.filter(Char::isDigit) },
            label = { Text(stringResource(R.string.parent_area_gate_answer_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.testTag("parent_gate_answer_field"),
        )
        if (showWrongAnswer) {
            Text(
                text = stringResource(R.string.parent_area_gate_wrong_answer),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        AdventureMenuButton(
            text = stringResource(R.string.parent_area_gate_submit),
            onClick = {
                if (answerText.toIntOrNull() == operandA + operandB) {
                    onUnlocked()
                } else {
                    showWrongAnswer = true
                    operandA = Random.nextInt(2, 10)
                    operandB = Random.nextInt(2, 10)
                    answerText = ""
                }
            },
            modifier = Modifier.padding(top = 24.dp).testTag("parent_gate_submit"),
        )
    }
}

@Composable
private fun ParentAreaSummary(
    uiState: ParentAreaUiState,
    onOpenSettings: () -> Unit,
    onResetConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ParentAreaStatRow(
                    label = stringResource(R.string.parent_area_chapters_completed_label),
                    value = "${uiState.chaptersCompleted} / ${uiState.totalChapters}",
                    valueTestTag = "parent_area_stat_chapters",
                )
                ParentAreaStatRow(
                    label = stringResource(R.string.parent_area_stars_label),
                    value = uiState.stars.toString(),
                    valueTestTag = "parent_area_stat_stars",
                )
                ParentAreaStatRow(
                    label = stringResource(R.string.parent_area_badges_label),
                    value = "${uiState.badgesEarned} / ${uiState.totalBadges}",
                    valueTestTag = "parent_area_stat_badges",
                )
                ParentAreaStatRow(
                    label = stringResource(R.string.parent_area_scripture_cards_label),
                    value = "${uiState.scriptureCardsEarned} / ${uiState.totalScriptureCards}",
                    valueTestTag = "parent_area_stat_scripture_cards",
                )
                ParentAreaStatRow(
                    label = stringResource(R.string.parent_area_time_played_label),
                    value = formatPlayTime(uiState.totalPlayTimeMillis),
                    valueTestTag = "parent_area_stat_time_played",
                )
            }
        }

        AdventureMenuButton(text = stringResource(R.string.parent_area_open_settings_label), onClick = onOpenSettings)
        AdventureMenuButton(
            text = stringResource(R.string.parent_area_view_privacy_label),
            onClick = { showPrivacyDialog = true },
        )
        Button(
            onClick = { showResetDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
        ) {
            Text(text = stringResource(R.string.parent_area_reset_progress_label), style = MaterialTheme.typography.labelLarge)
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(stringResource(R.string.parent_area_privacy_title)) },
            text = { Text(stringResource(R.string.parent_area_privacy_body)) },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(stringResource(R.string.parent_area_privacy_close))
                }
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.parent_area_reset_confirm_title)) },
            text = { Text(stringResource(R.string.parent_area_reset_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onResetConfirmed()
                    showResetDialog = false
                }) {
                    Text(stringResource(R.string.parent_area_reset_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.parent_area_reset_cancel_action))
                }
            },
        )
    }
}

@Composable
private fun ParentAreaStatRow(label: String, value: String, valueTestTag: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag(valueTestTag),
        )
    }
}

@Composable
private fun formatPlayTime(millis: Long): String {
    val totalMinutes = millis / 60_000L
    return when {
        totalMinutes < 1 -> stringResource(R.string.parent_area_time_played_less_than_a_minute)
        totalMinutes < 60 -> stringResource(R.string.parent_area_time_played_minutes, totalMinutes.toInt())
        else -> stringResource(
            R.string.parent_area_time_played_hours_minutes,
            (totalMinutes / 60).toInt(),
            (totalMinutes % 60).toInt(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ParentAreaLockedPreview() {
    BibleAdventuresTheme {
        ParentAreaContent(
            uiState = ParentAreaUiState(),
            onBack = {},
            onOpenSettings = {},
            onResetConfirmed = {},
        )
    }
}
