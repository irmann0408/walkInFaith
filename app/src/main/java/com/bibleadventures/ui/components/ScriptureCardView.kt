package com.bibleadventures.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp

/** Reusable card for displaying a scripture verse (Lesson scene, Reward scene, and later a Scripture Cards gallery). */
@Composable
fun ScriptureCardView(
    reference: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
            )
            Text(
                text = reference,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
