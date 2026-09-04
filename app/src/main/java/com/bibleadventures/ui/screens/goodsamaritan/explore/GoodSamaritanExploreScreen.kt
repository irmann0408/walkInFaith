package com.bibleadventures.ui.screens.goodsamaritan.explore

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.puzzles.dungeon.DungeonCombatState
import com.bibleadventures.game.puzzles.dungeon.DungeonGame
import com.bibleadventures.game.puzzles.dungeon.DungeonGameState
import com.bibleadventures.game.puzzles.dungeon.DungeonOutcome
import com.bibleadventures.game.puzzles.dungeon.Vector2
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.AspectRatioFitBox
import com.bibleadventures.ui.components.CharacterPreview
import com.bibleadventures.ui.components.Joystick
import com.bibleadventures.ui.components.Posture
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.goodsamaritan.GoodSamaritanViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlin.math.exp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val JOYSTICK_MAX_KNOB_TRAVEL = 32.dp

/** Cells visible across the (square) viewport at once — a window onto the map, not the whole 10x10 grid, so the camera actually has somewhere to pan. Tunable on-device. */
private const val VIEWPORT_CELLS = 5f

/** Exponential-ease rate the camera chases the player at, matching Hamsterholm's own `CAMERA_FOLLOW_SPEED` constant for its dungeon mode. */
private const val CAMERA_FOLLOW_SPEED = 4f

private const val PROJECTILE_FLIGHT_DURATION_MS = 350

/** Peak height (above the straight-line path) of the thrown supply's arc — a lob, not a flat slide. Tunable on-device. */
private val THROW_ARC_HEIGHT = 48.dp

/** How long the bandit's lunge toward the character takes, out and back. */
private const val BANDIT_LUNGE_OUT_DURATION_MS = 250
private const val BANDIT_LUNGE_BACK_DURATION_MS = 250

/** How far across the gap (as a fraction of the distance to the character) the bandit lunges — a leap, not a full swap of places. */
private const val BANDIT_LUNGE_FRACTION = 0.55f

/** How long the attack's result (stolen/missed) stays visible, at the peak of the lunge, before the bandit retreats back to idle. */
private const val BANDIT_ATTACK_RESULT_HOLD_MS = 500L

/** How long the final, defeating hit's projectile/toughness readout stays on screen before the overlay actually closes — long enough that the killing blow is never skipped past. */
private const val BANDIT_DEFEATED_HOLD_MS = 500L

@Composable
fun GoodSamaritanExploreScreen(
    viewModel: GoodSamaritanViewModel,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val characterCustomization by viewModel.characterCustomization.collectAsStateWithLifecycle()

    GoodSamaritanExploreContent(
        dungeonState = uiState.dungeonState,
        characterCustomization = characterCustomization,
        helpingBeatAcknowledged = uiState.helpingBeatAcknowledged,
        onDungeonTick = viewModel::onDungeonTick,
        onSupplyThrown = viewModel::onSupplyThrown,
        onBanditAttack = viewModel::onBanditAttack,
        onRetreat = viewModel::onRetreat,
        onHelpingBeatAcknowledged = viewModel::onHelpingBeatAcknowledged,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

@Composable
private fun GoodSamaritanExploreContent(
    dungeonState: DungeonGameState,
    characterCustomization: CharacterCustomization,
    helpingBeatAcknowledged: Boolean,
    onDungeonTick: (Vector2, Float) -> Unit,
    onSupplyThrown: () -> Unit,
    onBanditAttack: () -> Unit,
    onRetreat: () -> Unit,
    onHelpingBeatAcknowledged: () -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // The helping-beat and bandit-combat overlays both consume all touches
    // and have their own dedicated buttons — never show the top bar's own
    // Continue at the same time as either.
    val helpingBeatOverlayShowing = dungeonState.checkpointActivated && !helpingBeatAcknowledged
    val combat = dungeonState.combat

    // The overlay stays mounted a little *after* dungeonState.combat goes
    // null on a defeating hit, so that final throw's projectile/toughness
    // readout still gets to play out instead of the overlay vanishing the
    // instant the engine resolves — see BanditCombatOverlay's own
    // onFinished contract. Retreating (no animation in flight) calls
    // onFinished immediately instead.
    var combatOverlayVisible by remember { mutableStateOf(false) }
    LaunchedEffect(combat != null) {
        if (combat != null) combatOverlayVisible = true
    }

    // A MutableState *object* (not a plain Offset value) hoisted here and
    // passed by reference into Joystick — see that composable's own doc
    // comment for why a plain value parameter would go stale inside its
    // pointerInput closure.
    val knobOffsetState = remember { mutableStateOf(Offset.Zero) }
    val maxKnobTravelPx = with(LocalDensity.current) { JOYSTICK_MAX_KNOB_TRAVEL.toPx() }

    var cameraPosition by remember { mutableStateOf(dungeonState.playerPosition) }

    // Lets the frame loop below safely read the *latest* dungeonState even
    // though it's a plain parameter of this composable and the loop itself
    // (a LaunchedEffect keyed so it deliberately doesn't restart every
    // frame) would otherwise only ever see whatever dungeonState was on
    // the composition that started it — the same category of staleness
    // Joystick's own fix addresses, just for a parameter read directly
    // here instead of in a child composable.
    val latestDungeonState by rememberUpdatedState(dungeonState)

    LaunchedEffect(dungeonState.isComplete) {
        if (dungeonState.isComplete) return@LaunchedEffect
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
                onDungeonTick(Vector2(knobOffset.x / maxKnobTravelPx, knobOffset.y / maxKnobTravelPx), deltaSeconds)

                val target = latestDungeonState.playerPosition
                val followFactor = (1f - exp(-CAMERA_FOLLOW_SPEED * deltaSeconds)).coerceIn(0f, 1f)
                cameraPosition = Vector2(
                    x = cameraPosition.x + (target.x - cameraPosition.x) * followFactor,
                    y = cameraPosition.y + (target.y - cameraPosition.y) * followFactor,
                )
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if ((previouslyCompleted || dungeonState.isComplete) && !helpingBeatOverlayShowing && !combatOverlayVisible) {
                PuzzleTopBar(
                    showBackButton = previouslyCompleted,
                    onBackToMainMenu = onBackToMainMenu,
                    showNextButton = dungeonState.isComplete || previouslyCompleted,
                    onNext = onContinue,
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.good_samaritan_explore_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.good_samaritan_explore_instructions),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                )

                Box(modifier = Modifier.height(28.dp)) {
                    Text(
                        text = dungeonFeedbackText(dungeonState),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }

                DungeonWorld(
                    dungeonState = dungeonState,
                    cameraPosition = cameraPosition,
                    characterCustomization = characterCustomization,
                    modifier = Modifier.weight(1f, fill = true).fillMaxSize(),
                )

                Joystick(
                    knobOffsetState = knobOffsetState,
                    maxTravelPx = maxKnobTravelPx,
                    contentDescription = stringResource(R.string.good_samaritan_joystick_content_description),
                    modifier = Modifier.padding(top = 16.dp),
                )

                if (previouslyCompleted && !dungeonState.isComplete && !helpingBeatOverlayShowing) {
                    Text(
                        text = stringResource(R.string.puzzle_already_completed_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            if (combatOverlayVisible) {
                BanditCombatOverlay(
                    combat = combat,
                    supplyCount = dungeonState.supplyCount,
                    characterCustomization = characterCustomization,
                    lastOutcome = dungeonState.lastOutcome,
                    onSupplyThrown = onSupplyThrown,
                    onBanditAttack = onBanditAttack,
                    onRetreat = onRetreat,
                    onFinished = { combatOverlayVisible = false },
                )
            } else if (helpingBeatOverlayShowing) {
                HelpingBeatOverlay(onDismiss = onHelpingBeatAcknowledged)
            }
        }
    }
}

/** Reads as "Found medical supplies!" / "A bandit jumps out!" / "You stopped to help the traveler!" / "You reached the Inn!" — a live-region announcement plus visible feedback text, this screen's only non-visual feedback outside the bandit overlay. */
@Composable
private fun dungeonFeedbackText(dungeonState: DungeonGameState): String = when {
    dungeonState.isComplete -> stringResource(R.string.dungeon_feedback_goal_reached)
    dungeonState.lastOutcome == DungeonOutcome.CHECKPOINT_ACTIVATED -> stringResource(R.string.dungeon_feedback_checkpoint_activated)
    dungeonState.lastOutcome == DungeonOutcome.CHECKPOINT_NEEDS_SUPPLIES -> stringResource(R.string.dungeon_feedback_checkpoint_needs_supplies)
    dungeonState.lastOutcome == DungeonOutcome.SUPPLY_COLLECTED -> stringResource(R.string.dungeon_feedback_supply_collected)
    dungeonState.lastOutcome == DungeonOutcome.TRAP_ENTERED -> stringResource(R.string.dungeon_feedback_trap_entered)
    dungeonState.lastOutcome == DungeonOutcome.BANDIT_SCARED_OFF -> stringResource(R.string.dungeon_feedback_bandit_scared_off)
    dungeonState.lastOutcome == DungeonOutcome.RETREATED -> stringResource(R.string.dungeon_feedback_retreated)
    else -> ""
}

/**
 * Renders a [VIEWPORT_CELLS]-cell window around [cameraPosition] (clamped so
 * it never shows past the map edge), not the whole map — a real scrolling
 * camera, matching Hamsterholm's dungeon mode, rather than a fixed
 * full-board view. [AspectRatioFitBox] + [BoxWithConstraints] letterbox-fits
 * a square canvas exactly like every other grid-based screen in this app;
 * `cellSize` (a Dp, not a pixel value) lets every element's position and
 * size be plain Dp arithmetic, the same "offset(x = maxWidth * fraction,
 * ...)" idiom [com.bibleadventures.ui.screens.davidgoliath.slingpractice.DavidGoliathSlingPracticeScreen]
 * already uses. `clipToBounds()` is required here (unlike the old full-map
 * rendering) since walls/pickups near the viewport's edge would otherwise
 * visually spill past this Box — Compose doesn't clip overflowing children
 * by default.
 */
@Composable
private fun DungeonWorld(
    dungeonState: DungeonGameState,
    cameraPosition: Vector2,
    characterCustomization: CharacterCustomization,
    modifier: Modifier = Modifier,
) {
    val mapContentDescription = stringResource(R.string.good_samaritan_dungeon_map_content_description)

    AspectRatioFitBox(
        ratio = 1f,
        modifier = modifier,
        alignment = Alignment.TopCenter,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .semantics { contentDescription = mapContentDescription },
        ) {
            val cellSize = maxWidth / VIEWPORT_CELLS
            val halfViewport = VIEWPORT_CELLS / 2f
            val originX = cameraPosition.x.coerceIn(halfViewport, dungeonState.cols - halfViewport) - halfViewport
            val originY = cameraPosition.y.coerceIn(halfViewport, dungeonState.rows - halfViewport) - halfViewport
            val playerContentDescription = stringResource(R.string.good_samaritan_player_content_description)

            dungeonState.walls.forEachIndexed { row, wallRow ->
                wallRow.forEachIndexed { col, isWall ->
                    if (isWall) {
                        Image(
                            painter = painterResource(R.drawable.ic_wall_rock),
                            contentDescription = null,
                            modifier = Modifier
                                .offset(x = cellSize * (col - originX), y = cellSize * (row - originY))
                                .size(cellSize),
                        )
                    }
                }
            }

            dungeonState.supplies.forEach { supply ->
                if (supply.id !in dungeonState.collectedSupplyIds) {
                    CenteredCellIcon(R.drawable.ic_medicine, supply.position, cellSize, cellSize * 0.6f, originX, originY)
                }
            }

            dungeonState.traps.forEach { trap ->
                if (trap.id !in dungeonState.resolvedTrapIds) {
                    // A reduced scale, not a full cell — this sits on an
                    // open, walkable tile now (not a wall variant, like it
                    // used to be), so it shouldn't visually read as
                    // impassable. Placeholder art; real bandit art to
                    // follow later.
                    CenteredCellIcon(R.drawable.ic_wall_bandit, trap.position, cellSize, cellSize * 0.6f, originX, originY)
                }
            }

            if (!dungeonState.checkpointActivated) {
                CenteredCellIcon(R.drawable.ic_traveler_injured, dungeonState.checkpointPosition, cellSize, cellSize * 0.7f, originX, originY)
            }

            CenteredCellIcon(R.drawable.ic_inn, dungeonState.goalPosition, cellSize, cellSize * 0.8f, originX, originY)

            // The player's own customized character, not a generic marker —
            // CharacterPreview applies `modifier.size(160.dp)` to whatever
            // modifier it's given, so a smaller `.size(...)` supplied here
            // constrains it first and the internal 160dp request is
            // clamped down to fit (same trick CharacterCallout already
            // relies on to show it at 96dp).
            CharacterPreview(
                customization = characterCustomization,
                posture = Posture.STANDING,
                modifier = Modifier
                    .offset(
                        x = cellSize * (dungeonState.playerPosition.x - originX) - cellSize * 0.4f,
                        y = cellSize * (dungeonState.playerPosition.y - originY) - cellSize * 0.4f,
                    )
                    .size(cellSize * 0.8f)
                    .semantics { contentDescription = playerContentDescription },
            )
        }
    }
}

@Composable
private fun CenteredCellIcon(drawableRes: Int, position: Vector2, cellSize: Dp, iconSize: Dp, originX: Float, originY: Float) {
    Image(
        painter = painterResource(drawableRes),
        contentDescription = null,
        modifier = Modifier
            .offset(
                x = cellSize * (position.x - originX) - iconSize / 2,
                y = cellSize * (position.y - originY) - iconSize / 2,
            )
            .size(iconSize),
    )
}

private enum class BanditPose { IDLE, ATTACKING }

/**
 * A confirmed, explicit exception to this app's normal "no combat / no
 * failure states" rule (see `docs/PROJECT_STATUS.md`'s Good Samaritan
 * dungeon addendum) — kept as gentle as the app's only other exception
 * (David & Goliath's Connect Four): the bandit's counter-attack never hurts
 * the player, only risks a stolen supply, and running out of supplies never
 * ends the run — Retreat leaves the bandit for later with nothing lost but
 * the supplies already spent.
 *
 * Both sides now roll (see [DungeonGame.PLAYER_HIT_CHANCE]/[DungeonGame.BANDIT_STEAL_CHANCE]):
 * tapping the character commits the throw to game state *immediately*
 * (unlike Sling Practice's deferred-until-animation-lands pattern, which
 * exists there because the outcome depends on a *moving* target's future
 * position — here both rolls are already resolved by the time any
 * animation starts), and the bandit's own counter-attack is triggered by
 * this composable itself once the throw's projectile animation lands,
 * *if* the bandit is still around to make it (a defeating hit clears
 * `combat` immediately, and the counter-attack step below checks the live
 * value before proceeding).
 *
 * [combat] is nullable specifically so the *defeating* throw's projectile
 * still gets to visually land: the engine clears `combat` the instant a
 * hit resolves, but this overlay needs to keep rendering (using
 * [displayedCombat], its own last-known-good snapshot) until its own
 * animation sequence finishes, then calls [onFinished] to tell the parent
 * it's safe to actually unmount — otherwise the final blow would cut the
 * overlay off mid-animation, which is exactly the "fight just vanishes on
 * the 2nd tap" bug this fixes.
 */
@Composable
private fun BanditCombatOverlay(
    combat: DungeonCombatState?,
    supplyCount: Int,
    characterCustomization: CharacterCustomization,
    lastOutcome: DungeonOutcome,
    onSupplyThrown: () -> Unit,
    onBanditAttack: () -> Unit,
    onRetreat: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayedCombat by remember { mutableStateOf(combat) }
    LaunchedEffect(combat) {
        if (combat != null) displayedCombat = combat
    }
    val shownCombat = displayedCombat ?: return

    var throwTrigger by remember { mutableIntStateOf(0) }
    var isResolving by remember { mutableStateOf(false) }
    var banditPose by remember { mutableStateOf(BanditPose.IDLE) }
    val flightProgress = remember { Animatable(1f) }
    // 0 = at its home spot, 1 = fully lunged toward the character — driven
    // out and back around the steal roll, not a static pose swap.
    val banditLungeProgress = remember { Animatable(0f) }
    val latestCombat by rememberUpdatedState(combat)

    LaunchedEffect(throwTrigger) {
        if (throwTrigger == 0) return@LaunchedEffect
        isResolving = true
        flightProgress.snapTo(0f)
        flightProgress.animateTo(1f, animationSpec = tween(PROJECTILE_FLIGHT_DURATION_MS))

        if (latestCombat != null) {
            // The bandit survived the hit (or the throw missed) — it lunges
            // toward the character like a thrown attack, the steal roll
            // resolves at the peak of the lunge, then it retreats back to
            // its spot and settles to idle.
            banditPose = BanditPose.ATTACKING
            banditLungeProgress.animateTo(1f, animationSpec = tween(BANDIT_LUNGE_OUT_DURATION_MS))
            onBanditAttack()
            delay(BANDIT_ATTACK_RESULT_HOLD_MS)
            banditLungeProgress.animateTo(0f, animationSpec = tween(BANDIT_LUNGE_BACK_DURATION_MS))
            banditPose = BanditPose.IDLE
            isResolving = false
        } else {
            // Defeated: hold the final frame briefly (toughness now reads
            // 0, the projectile has visibly landed) before telling the
            // parent it's safe to unmount this overlay.
            delay(BANDIT_DEFEATED_HOLD_MS)
            onFinished()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard(
            modifier = Modifier.widthIn(max = 400.dp).padding(24.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.good_samaritan_bandit_encounter_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.good_samaritan_bandit_toughness_label, shownCombat.banditToughnessRemaining, DungeonGame.BANDIT_INITIAL_TOUGHNESS),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = stringResource(R.string.good_samaritan_supply_count_label, supplyCount),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                val combatFeedback = when (lastOutcome) {
                    DungeonOutcome.BANDIT_HIT -> stringResource(R.string.dungeon_feedback_bandit_hit)
                    DungeonOutcome.THROW_MISSED -> stringResource(R.string.dungeon_feedback_throw_missed)
                    DungeonOutcome.SUPPLY_STOLEN -> stringResource(R.string.dungeon_feedback_supply_stolen)
                    DungeonOutcome.BANDIT_ATTACK_MISSED -> stringResource(R.string.dungeon_feedback_bandit_attack_missed)
                    DungeonOutcome.OUT_OF_SUPPLIES -> stringResource(R.string.dungeon_feedback_out_of_supplies)
                    else -> ""
                }
                Box(modifier = Modifier.height(24.dp)) {
                    Text(
                        text = combatFeedback,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }

                // Bandit and character render at the same size — "the bandit
                // should be at least as big as our character."
                val combatSpriteSize = 120.dp
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(140.dp).padding(top = 8.dp)) {
                    val characterCenterX = combatSpriteSize / 2
                    val banditHomeCenterX = maxWidth - combatSpriteSize / 2
                    val banditLungeCenterX = banditHomeCenterX - (banditHomeCenterX - characterCenterX) * BANDIT_LUNGE_FRACTION
                    val banditCenterX = banditHomeCenterX + (banditLungeCenterX - banditHomeCenterX) * banditLungeProgress.value
                    val spriteTopY = (maxHeight - combatSpriteSize) / 2

                    Image(
                        painter = painterResource(if (banditPose == BanditPose.ATTACKING) R.drawable.ic_bandit_attack else R.drawable.ic_bandit_idle),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = banditCenterX - combatSpriteSize / 2, y = spriteTopY)
                            .size(combatSpriteSize),
                    )

                    if (supplyCount > 0) {
                        val throwDescription = stringResource(R.string.good_samaritan_throw_supply_content_description)
                        CharacterPreview(
                            customization = characterCustomization,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(combatSpriteSize)
                                .clickable(enabled = !isResolving, onClickLabel = throwDescription) {
                                    throwTrigger++
                                    onSupplyThrown()
                                }
                                // A distinct content description from CharacterPreview's own
                                // built-in one: the map behind this overlay also shows a
                                // CharacterPreview (still composed, just visually covered by
                                // this scrim), so without an override both nodes would report
                                // the same generic description and any lookup expecting a
                                // single match (real accessibility tooling or this app's own
                                // instrumented test) would find two.
                                .semantics(mergeDescendants = true) { contentDescription = throwDescription },
                        )
                    }

                    if (flightProgress.value < 1f) {
                        val progress = flightProgress.value
                        // The projectile always flies at the bandit's home
                        // spot — it's still there (or already retreating
                        // back to it) whenever a new throw lands, since a
                        // throw can only happen once the previous exchange
                        // (including any lunge) has fully settled.
                        val projectileX = characterCenterX + (banditHomeCenterX - characterCenterX) * progress
                        // A thrown-underhand lob, not a flat straight-line
                        // slide: a parabola peaking at the midpoint
                        // (progress = 0.5) and back to baseline at both
                        // ends, scaled by THROW_ARC_HEIGHT.
                        val arcLift = THROW_ARC_HEIGHT * 4f * progress * (1f - progress)
                        Image(
                            painter = painterResource(R.drawable.ic_medicine),
                            contentDescription = null,
                            modifier = Modifier
                                .offset(x = projectileX - 16.dp, y = maxHeight / 2 - 16.dp - arcLift)
                                .size(32.dp),
                        )
                    }
                }

                if (supplyCount == 0) {
                    Text(
                        text = stringResource(R.string.good_samaritan_out_of_supplies_message),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
                    )
                    AdventureMenuButton(
                        text = stringResource(R.string.good_samaritan_retreat_button),
                        onClick = {
                            onRetreat()
                            onFinished()
                        },
                    )
                }
            }
        }
    }
}

/**
 * An automatic story beat, not a Choice scene — Luke 10:34 describes a
 * specific, non-branching sequence of care, so there's nothing real to pick.
 * Consumes all touches so the joystick underneath can't be dragged while
 * it's up.
 */
@Composable
private fun HelpingBeatOverlay(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard(
            modifier = Modifier.widthIn(max = 400.dp).padding(24.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.good_samaritan_helping_beat_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Column(
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GoodSamaritanContent.helpingBeatLines.forEach { lineRes ->
                        Text(text = stringResource(lineRes), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                AdventureMenuButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GoodSamaritanExplorePreview() {
    BibleAdventuresTheme {
        GoodSamaritanExploreContent(
            dungeonState = DungeonGame.fromLayout(GoodSamaritanContent.mapLayout),
            characterCustomization = CharacterCustomization(),
            helpingBeatAcknowledged = false,
            onDungeonTick = { _, _ -> },
            onSupplyThrown = {},
            onBanditAttack = {},
            onRetreat = {},
            onHelpingBeatAcknowledged = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
