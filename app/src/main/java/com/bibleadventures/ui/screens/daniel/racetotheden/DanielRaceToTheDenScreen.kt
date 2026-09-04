package com.bibleadventures.ui.screens.daniel.racetotheden

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.puzzles.racemaze.RaceMazeGame
import com.bibleadventures.game.puzzles.racemaze.RaceMazeGameState
import com.bibleadventures.game.puzzles.racemaze.Vector2
import com.bibleadventures.game.stories.DanielContent
import com.bibleadventures.ui.components.AspectRatioFitBox
import com.bibleadventures.ui.components.CharacterPreview
import com.bibleadventures.ui.components.Joystick
import com.bibleadventures.ui.components.Posture
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.daniel.DanielViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.isActive

private val JOYSTICK_MAX_KNOB_TRAVEL = 32.dp

/**
 * The maze artwork's playable interior as fractions of the full
 * `bg_daniel_race_to_the_den_maze` image — measured directly from "Race to
 * the Den Maze outline 2.png", a red-ink tracing laid at the same size and
 * crop as the real background art (see `DanielContent`'s own doc comment on
 * the maze data), so these fractions and the wall grid they pair with come
 * from the same pixel space. Used to map [RaceMazeGameState]'s cell-unit
 * [RaceMazeGameState.playerPosition] onto the actual drawn corridors.
 */
private const val MAZE_INTERIOR_X0 = 49.36f / 412f
private const val MAZE_INTERIOR_X1 = 364.93f / 412f
private const val MAZE_INTERIOR_Y0 = 56.04f / 423f
private const val MAZE_INTERIOR_Y1 = 376.68f / 423f

/**
 * As a fraction of one maze cell's own width. Deliberately smaller than a
 * full cell: this maze's corridors are only about one cell wide even before
 * a wall's own drawn thickness narrows them further, so a sprite sized at
 * or above a full cell width would visually overlap the walls on both
 * sides of every corridor it stands in — reading as "walking through
 * walls" even on frames where the actual collision point never crosses
 * one. Tunable on-device.
 */
private const val CHARACTER_SIZE_FRACTION_OF_CELL = 0.85f

/**
 * How far down the character sprite's own height its logical collision
 * point (the [RaceMazeGameState.playerPosition] this screen renders at)
 * sits — 1.0 would anchor at the very bottom, 0.5 would center it. The
 * sprite includes a fair amount of headroom above the body, so anchoring
 * at the vertical center (the naive choice) makes the character visually
 * "sink" past a wall's line by roughly half its own height; anchoring
 * closer to the feet keeps the feet on the correct side of a drawn wall
 * even though the head then reads as floating slightly above it — an
 * acceptable trade for a top-down maze sprite. Tunable on-device.
 */
private const val CHARACTER_FEET_ANCHOR_FRACTION = 0.85f

/**
 * Daniel 6:19, Darius's dawn rush to the lions' den — reworked from the
 * older D-pad "Darius's Maze" (blocky `game/puzzles/gridmaze` grid) into a
 * real hand-drawn corridor maze navigated with the same real-time analog
 * joystick as
 * [com.bibleadventures.ui.screens.goodsamaritan.explore.GoodSamaritanExploreScreen]'s
 * "mini dungeon", via the purpose-built [RaceMazeGame] engine (a thin-wall
 * collision adaptation — this art's walls are drawn ON cell boundaries, not
 * whole blocked cells). Unlike the dungeon's scrolling 5x5 viewport, this
 * maze is small enough to show uncropped, so there's no camera-follow logic
 * here at all.
 */
@Composable
fun DanielRaceToTheDenScreen(
    viewModel: DanielViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val characterCustomization by viewModel.characterCustomization.collectAsStateWithLifecycle()

    DanielRaceToTheDenContent(
        raceMazeState = uiState.raceMazeState,
        characterCustomization = characterCustomization,
        onRaceMazeTick = viewModel::onRaceMazeTick,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun DanielRaceToTheDenContent(
    raceMazeState: RaceMazeGameState,
    characterCustomization: CharacterCustomization,
    onRaceMazeTick: (Vector2, Float) -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Same hoisted-MutableState-object joystick pattern as
    // GoodSamaritanExploreScreen — see Joystick's own doc comment for why a
    // plain Offset parameter would go stale inside its pointerInput closure.
    val knobOffsetState = remember { mutableStateOf(Offset.Zero) }
    val maxKnobTravelPx = with(LocalDensity.current) { JOYSTICK_MAX_KNOB_TRAVEL.toPx() }

    // No camera-follow here (unlike GoodSamaritanExploreContent) — this
    // maze is small enough to show uncropped, so the loop only needs to
    // feed the joystick reading into the engine each frame.
    LaunchedEffect(raceMazeState.isComplete) {
        if (raceMazeState.isComplete) return@LaunchedEffect
        var previousFrameNanos = -1L
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (previousFrameNanos < 0) {
                    previousFrameNanos = frameNanos
                    return@withFrameNanos
                }
                val deltaSeconds = (frameNanos - previousFrameNanos) / 1_000_000_000f
                previousFrameNanos = frameNanos

                val knobOffset = knobOffsetState.value
                onRaceMazeTick(Vector2(knobOffset.x / maxKnobTravelPx, knobOffset.y / maxKnobTravelPx), deltaSeconds)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (previouslyCompleted || raceMazeState.isComplete) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = raceMazeState.isComplete || previouslyCompleted,
                    onNext = onContinue,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.daniel_race_to_the_den_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.daniel_race_to_the_den_instructions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )

            Box(modifier = Modifier.height(28.dp)) {
                Text(
                    text = raceMazeFeedbackText(raceMazeState),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            RaceMazeWorld(
                raceMazeState = raceMazeState,
                characterCustomization = characterCustomization,
                modifier = Modifier.weight(1f, fill = true).fillMaxSize(),
            )

            Joystick(
                knobOffsetState = knobOffsetState,
                maxTravelPx = maxKnobTravelPx,
                contentDescription = stringResource(R.string.daniel_race_to_the_den_joystick_content_description),
                modifier = Modifier.padding(top = 16.dp),
            )

            if (previouslyCompleted && !raceMazeState.isComplete) {
                Text(
                    text = stringResource(R.string.puzzle_already_completed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** Reads as "You reached the lions' den!" — this screen's only non-visual feedback. */
@Composable
private fun raceMazeFeedbackText(raceMazeState: RaceMazeGameState): String =
    if (raceMazeState.isComplete) stringResource(R.string.daniel_race_to_the_den_feedback_goal_reached) else ""

/**
 * The whole maze art shown uncropped (no scrolling camera, unlike the
 * dungeon's viewport — this 14x14 maze fits on screen at once) with the
 * player's own customized character overlaid, positioned by mapping
 * [RaceMazeGameState.playerPosition] (cell units) through the art's
 * calibrated interior fraction box.
 */
@Composable
private fun RaceMazeWorld(
    raceMazeState: RaceMazeGameState,
    characterCustomization: CharacterCustomization,
    modifier: Modifier = Modifier,
) {
    val mapContentDescription = stringResource(R.string.daniel_race_to_the_den_map_content_description)
    val playerContentDescription = stringResource(R.string.daniel_race_to_the_den_player_content_description)

    AspectRatioFitBox(
        ratio = 1f,
        modifier = modifier,
        alignment = Alignment.TopCenter,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = mapContentDescription },
        ) {
            Image(
                painter = painterResource(R.drawable.bg_daniel_race_to_the_den_maze),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )

            val cellFractionX = (MAZE_INTERIOR_X1 - MAZE_INTERIOR_X0) / DanielContent.RACE_MAZE_SIZE
            val characterSize = maxWidth * cellFractionX * CHARACTER_SIZE_FRACTION_OF_CELL
            val positionFractionX = MAZE_INTERIOR_X0 + (raceMazeState.playerPosition.x / DanielContent.RACE_MAZE_SIZE) * (MAZE_INTERIOR_X1 - MAZE_INTERIOR_X0)
            val positionFractionY = MAZE_INTERIOR_Y0 + (raceMazeState.playerPosition.y / DanielContent.RACE_MAZE_SIZE) * (MAZE_INTERIOR_Y1 - MAZE_INTERIOR_Y0)

            CharacterPreview(
                customization = characterCustomization,
                posture = Posture.STANDING,
                modifier = Modifier
                    .offset(
                        x = maxWidth * positionFractionX - characterSize / 2,
                        y = maxHeight * positionFractionY - characterSize * CHARACTER_FEET_ANCHOR_FRACTION,
                    )
                    .size(characterSize)
                    .semantics { contentDescription = playerContentDescription },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DanielRaceToTheDenPreview() {
    BibleAdventuresTheme {
        DanielRaceToTheDenContent(
            raceMazeState = RaceMazeGame.fromWalls(
                DanielContent.raceMazeVerticalWalls,
                DanielContent.raceMazeHorizontalWalls,
                DanielContent.raceMazeStart,
                DanielContent.raceMazeGoal,
            ),
            characterCustomization = CharacterCustomization(),
            onRaceMazeTick = { _, _ -> },
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
