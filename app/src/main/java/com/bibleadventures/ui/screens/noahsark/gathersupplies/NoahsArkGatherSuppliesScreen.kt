package com.bibleadventures.ui.screens.noahsark.gathersupplies

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.game.stories.NoahsArkContent
import com.bibleadventures.game.stories.SupplyDef
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.screens.noahsark.NoahsArkViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme

@Composable
fun NoahsArkGatherSuppliesScreen(
    viewModel: NoahsArkViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NoahsArkGatherSuppliesContent(
        collectedSupplyIds = uiState.collectedSupplyIds,
        onSupplyTapped = viewModel::onSupplyCollected,
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun NoahsArkGatherSuppliesContent(
    collectedSupplyIds: Set<String>,
    onSupplyTapped: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allCollected = collectedSupplyIds.size == NoahsArkContent.supplies.size

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.noahs_ark_gather_supplies_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.noahs_ark_gather_supplies_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                NoahsArkContent.supplies.forEach { supply ->
                    SupplyTile(
                        supply = supply,
                        isCollected = supply.id in collectedSupplyIds,
                        onClick = { onSupplyTapped(supply.id) },
                    )
                }
            }

            if (allCollected) {
                AdventureMenuButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onContinue,
                    modifier = Modifier.padding(top = 32.dp),
                )
            }
        }
    }
}

@Composable
private fun SupplyTile(supply: SupplyDef, isCollected: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(targetValue = if (isCollected) 0.5f else 1f, label = "supplyCollectedAlpha")
    val name = stringResource(supply.nameRes)

    Box(
        modifier = Modifier
            .size(72.dp)
            .clickable(enabled = !isCollected, onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(supply.iconRes),
            contentDescription = null,
            modifier = Modifier.size(64.dp).alpha(alpha),
        )
        if (isCollected) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoahsArkGatherSuppliesPreview() {
    BibleAdventuresTheme {
        NoahsArkGatherSuppliesContent(collectedSupplyIds = emptySet(), onSupplyTapped = {}, onContinue = {})
    }
}
