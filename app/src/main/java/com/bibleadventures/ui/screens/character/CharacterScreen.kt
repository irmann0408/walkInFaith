package com.bibleadventures.ui.screens.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bibleadventures.R
import com.bibleadventures.character.CharacterOptionCatalog
import com.bibleadventures.domain.model.CHARACTER_STYLE_ILLUSTRATED_ENABLED
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.domain.model.CharacterStyle
import com.bibleadventures.ui.AppViewModelProvider
import com.bibleadventures.ui.components.CharacterPreview
import com.bibleadventures.ui.screens.character.components.OptionPicker
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun CharacterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CharacterViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CharacterContent(
        customization = uiState.customization,
        onBack = onBack,
        onAppearanceSelected = viewModel::onAppearanceSelected,
        onHairstyleSelected = viewModel::onHairstyleSelected,
        onSkinToneSelected = viewModel::onSkinToneSelected,
        onClothingSelected = viewModel::onClothingSelected,
        onCharacterStyleSelected = viewModel::onCharacterStyleSelected,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterContent(
    customization: CharacterCustomization,
    onBack: () -> Unit,
    onAppearanceSelected: (com.bibleadventures.domain.model.Appearance) -> Unit,
    onHairstyleSelected: (com.bibleadventures.domain.model.Hairstyle) -> Unit,
    onSkinToneSelected: (com.bibleadventures.domain.model.SkinTone) -> Unit,
    onClothingSelected: (com.bibleadventures.domain.model.Clothing) -> Unit,
    onCharacterStyleSelected: (com.bibleadventures.domain.model.CharacterStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.character_screen_title)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CharacterPreview(customization = customization)
                }
            }
            // Character Style (Classic/Illustrated) picker parked for v2.0 —
            // the underlying CharacterStyle enum, CharacterPreview's
            // Illustrated rendering branch, and all art files stay intact;
            // only this picker row is hidden, so characterStyle can never
            // leave its CLASSIC default for a fresh install. Re-enable by
            // restoring this item block (see docs/PROJECT_STATUS.md).
            item {
                OptionPicker(
                    title = stringResource(R.string.character_section_appearance),
                    options = CharacterOptionCatalog.appearances.map { it.value },
                    selectedOption = customization.appearance,
                    label = { value -> CharacterOptionCatalog.appearances.first { it.value == value }.let { stringResource(it.labelRes) } },
                    onOptionSelected = onAppearanceSelected,
                )
            }
            item {
                // Hairstyle has real art for every value in both styles now
                // (Illustrated has a full render per hairstyle, Classic
                // draws it directly) — shown unconditionally.
                OptionPicker(
                    title = stringResource(R.string.character_section_hairstyle),
                    options = CharacterOptionCatalog.hairstyles.map { it.value },
                    selectedOption = customization.hairstyle,
                    label = { value -> CharacterOptionCatalog.hairstyles.first { it.value == value }.let { stringResource(it.labelRes) } },
                    onOptionSelected = onHairstyleSelected,
                )
            }
            // Effectively-Classic check, not a raw equality check: a profile
            // saved before CHARACTER_STYLE_ILLUSTRATED_ENABLED existed could
            // still have characterStyle = ILLUSTRATED persisted even though
            // the picker that could set it is hidden — Skin Tone must still
            // show for those devices too, matching CharacterPreview's own
            // gate, or they'd be stuck with neither Skin Tone nor real
            // Illustrated art.
            if (customization.characterStyle == CharacterStyle.CLASSIC || !CHARACTER_STYLE_ILLUSTRATED_ENABLED) {
                item {
                    // Skin Tone has no illustrated art variants yet — Classic only.
                    OptionPicker(
                        title = stringResource(R.string.character_section_skin_tone),
                        options = CharacterOptionCatalog.skinTones.map { it.value },
                        selectedOption = customization.skinTone,
                        label = { value -> CharacterOptionCatalog.skinTones.first { it.value == value }.let { stringResource(it.labelRes) } },
                        swatchColor = { value -> CharacterOptionCatalog.skinTones.first { it.value == value }.swatchColor },
                        onOptionSelected = onSkinToneSelected,
                    )
                }
            }
            item {
                OptionPicker(
                    title = stringResource(R.string.character_section_clothing),
                    options = CharacterOptionCatalog.clothingOptions.map { it.value },
                    selectedOption = customization.clothing,
                    label = { value -> CharacterOptionCatalog.clothingOptions.first { it.value == value }.let { stringResource(it.labelRes) } },
                    swatchColor = { value -> CharacterOptionCatalog.clothingOptions.first { it.value == value }.swatchColor },
                    onOptionSelected = onClothingSelected,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterScreenPreview() {
    BibleAdventuresTheme {
        CharacterContent(
            customization = CharacterCustomization(),
            onBack = {},
            onAppearanceSelected = {},
            onHairstyleSelected = {},
            onSkinToneSelected = {},
            onClothingSelected = {},
            onCharacterStyleSelected = {},
        )
    }
}
