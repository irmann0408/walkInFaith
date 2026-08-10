package com.bibleadventures.ui.screens.character.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bibleadventures.R

/**
 * One picker section (a title + a row of selectable swatches). Selection is
 * never communicated by color alone (spec section 13) — a check icon and
 * content description also mark the selected option.
 */
@Composable
fun <T> OptionPicker(
    title: String,
    options: List<T>,
    selectedOption: T,
    label: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    swatchColor: (@Composable (T) -> Color)? = null,
) {
    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                val optionLabel = label(option)
                val selectedDescription = if (isSelected) {
                    stringResource(R.string.character_option_selected_content_description, optionLabel)
                } else {
                    optionLabel
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OptionSwatch(
                        color = swatchColor?.invoke(option) ?: MaterialTheme.colorScheme.surfaceVariant,
                        isSelected = isSelected,
                        contentDescription = selectedDescription,
                        onClick = { onOptionSelected(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionSwatch(
    color: Color,
    isSelected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(color)
            .border(3.dp, borderColor, CircleShape)
            .clickable(onClickLabel = contentDescription) { onClick() }
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.RadioButton
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
