package com.bibleadventures.ui.screens.goodsamaritan.explore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibleadventures.R
import com.bibleadventures.audio.CharacterVoiceLine
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.puzzles.dungeon.DungeonGame
import com.bibleadventures.game.puzzles.dungeon.DungeonGameState
import com.bibleadventures.game.puzzles.dungeon.DungeonOutcome
import com.bibleadventures.game.puzzles.dungeon.Vector2
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.ui.LocalAudioController
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.AspectRatioFitBox
import com.bibleadventures.ui.components.CharacterCallout
import com.bibleadventures.ui.components.CharacterPreview
import com.bibleadventures.ui.components.Joystick
import com.bibleadventures.ui.components.Posture
import com.bibleadventures.ui.components.PuzzleTopBar
import com.bibleadventures.ui.screens.goodsamaritan.GoodSamaritanViewModel
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val JOYSTICK_MAX_KNOB_TRAVEL = 32.dp

/** This app's minimum tappable-game-object size (spec accessibility rule) — the medical supply icon, even doubled, is still smaller than this at this map's zoom level. */
private val MIN_TAP_TARGET_SIZE = 48.dp

/**
 * Cells visible across the (square) viewport at once — a window onto the
 * map, not the whole grid, so the camera actually has somewhere to pan.
 * Raised from the original map's `5f` once the 56x30 road map replaced the
 * old 10x10 placeholder: at `5f`, the viewport only ever showed ~9% of the
 * road's width at once, so the source art's own rocks/terrain (drawn at a
 * scale meant to be seen as a wider overview, not a tight close-up) came out
 * looking blown up next to the character. A larger value shows proportionally
 * more of the map per frame instead of magnifying a tiny sliver of it.
 * Tunable on-device.
 */
private const val VIEWPORT_CELLS = 14f

/** Exponential-ease rate the camera chases the player at, matching Hamsterholm's own `CAMERA_FOLLOW_SPEED` constant for its dungeon mode. */
private const val CAMERA_FOLLOW_SPEED = 4f

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
        medicalSupplyPreviewAcknowledged = uiState.medicalSupplyPreviewAcknowledged,
        banditPreviewAcknowledged = uiState.banditPreviewAcknowledged,
        onDungeonTick = viewModel::onDungeonTick,
        onSupplyThrown = viewModel::onSupplyThrown,
        onSamaritanAttack = viewModel::onSamaritanAttack,
        onBanditAttack = viewModel::onBanditAttack,
        onRetreat = viewModel::onRetreat,
        onHelpingBeatAcknowledged = viewModel::onHelpingBeatAcknowledged,
        onMedicalSupplyPreviewAcknowledged = viewModel::onMedicalSupplyPreviewAcknowledged,
        onBanditPreviewAcknowledged = viewModel::onBanditPreviewAcknowledged,
        onContinue = onContinue,
        onBackToMainMenu = onBackToMainMenu,
        previouslyCompleted = previouslyCompleted,
        modifier = modifier,
    )
}

/** Which kind of map object a deliberate tap should explain — see [ItemPreviewOverlay]. */
private enum class MapItemPreview { SUPPLY, BANDIT, TRAVELER }

@Composable
private fun GoodSamaritanExploreContent(
    dungeonState: DungeonGameState,
    characterCustomization: CharacterCustomization,
    helpingBeatAcknowledged: Boolean,
    medicalSupplyPreviewAcknowledged: Boolean,
    banditPreviewAcknowledged: Boolean,
    onDungeonTick: (Vector2, Float) -> Unit,
    onSupplyThrown: () -> Unit,
    onSamaritanAttack: () -> Unit,
    onBanditAttack: () -> Unit,
    onRetreat: () -> Unit,
    onHelpingBeatAcknowledged: () -> Unit,
    onMedicalSupplyPreviewAcknowledged: () -> Unit,
    onBanditPreviewAcknowledged: () -> Unit,
    onContinue: () -> Unit,
    onBackToMainMenu: () -> Unit,
    previouslyCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val audioController = LocalAudioController.current

    // The helping-beat, bandit-combat, and item-preview overlays all consume
    // all touches and have their own dedicated buttons — never show the top
    // bar's own Continue at the same time as any of them.
    val helpingBeatOverlayShowing = dungeonState.checkpointActivated && !helpingBeatAcknowledged
    val combat = dungeonState.combat

    // Shown automatically exactly once — the first time the player ever
    // collects a supply, or is ever ambushed — and takes priority over the
    // bandit fight itself so a brand-new player gets the explainer before
    // being thrown into combat, not mid-fight. A deliberate tap on either
    // item's own map icon (see [tappedPreview]) shows this same content on
    // demand afterward, and also flips the matching acknowledged flag so
    // the automatic version never redundantly repeats it.
    val showBanditIntro = combat != null && !banditPreviewAcknowledged
    val showSupplyIntro = dungeonState.collectedSupplyIds.isNotEmpty() && !medicalSupplyPreviewAcknowledged
    var tappedPreview by remember { mutableStateOf<MapItemPreview?>(null) }

    // The overlay stays mounted a little *after* dungeonState.combat goes
    // null on a defeating hit, so that final throw's projectile/toughness
    // readout still gets to play out instead of the overlay vanishing the
    // instant the engine resolves — see BanditPartyBattleOverlay's own
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

    // True whenever any full-screen overlay is covering the map/joystick —
    // the top bar's own Continue must never show at the same time as one.
    val anyOverlayShowing = helpingBeatOverlayShowing || combatOverlayVisible || showBanditIntro || showSupplyIntro || tappedPreview != null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if ((previouslyCompleted || dungeonState.isComplete) && !anyOverlayShowing) {
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

                Box(modifier = Modifier.weight(1f, fill = true).fillMaxSize()) {
                    DungeonWorld(
                        dungeonState = dungeonState,
                        cameraPosition = cameraPosition,
                        characterCustomization = characterCustomization,
                        onMapItemTapped = { tappedPreview = it },
                        modifier = Modifier.fillMaxSize(),
                    )

                    // A one-time flavor line orienting a first-time player —
                    // "here's the road the parable's own travelers walked."
                    // Auto-dismisses after a few seconds rather than sitting
                    // over the map indefinitely.
                    var showIntroMessage by remember { mutableStateOf(true) }
                    val introMessage = stringResource(R.string.good_samaritan_explore_intro_message)
                    LaunchedEffect(Unit) {
                        audioController.playCharacterLine(CharacterVoiceLine.GOOD_SAMARITAN_EXPLORE_INTRO)
                        delay(6_000)
                        showIntroMessage = false
                    }
                    if (showIntroMessage) {
                        CharacterCallout(
                            characterCustomization = characterCustomization,
                            message = introMessage,
                            posture = Posture.STANDING,
                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                        )
                    }
                }

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

            when {
                // A deliberate tap always wins — the player asked for this
                // explanation right now, regardless of what else is going on
                // (reachable in practice only when nothing else is already
                // covering the map, since every other overlay itself
                // consumes touches before a map icon ever could).
                tappedPreview != null -> {
                    val preview = tappedPreview
                    val onDismiss = {
                        when (preview) {
                            MapItemPreview.SUPPLY -> onMedicalSupplyPreviewAcknowledged()
                            MapItemPreview.BANDIT -> onBanditPreviewAcknowledged()
                            MapItemPreview.TRAVELER, null -> Unit
                        }
                        tappedPreview = null
                    }
                    when (preview) {
                        MapItemPreview.SUPPLY -> ItemPreviewOverlay(
                            imageRes = R.drawable.ic_medicine,
                            imageContentDescription = stringResource(R.string.good_samaritan_supply_content_description),
                            title = stringResource(R.string.good_samaritan_supply_preview_title),
                            description = stringResource(R.string.good_samaritan_supply_preview_description),
                            onDismiss = onDismiss,
                        )
                        MapItemPreview.BANDIT -> ItemPreviewOverlay(
                            imageRes = R.drawable.ic_bandit_idle,
                            imageContentDescription = stringResource(R.string.good_samaritan_bandit_content_description),
                            title = stringResource(R.string.good_samaritan_bandit_preview_title),
                            description = stringResource(R.string.good_samaritan_bandit_preview_description),
                            onDismiss = onDismiss,
                        )
                        MapItemPreview.TRAVELER -> ItemPreviewOverlay(
                            imageRes = R.drawable.ic_traveler_injured,
                            imageContentDescription = stringResource(R.string.good_samaritan_traveler_content_description),
                            title = stringResource(R.string.good_samaritan_traveler_preview_title),
                            description = stringResource(R.string.good_samaritan_traveler_preview_description),
                            onDismiss = onDismiss,
                        )
                        null -> Unit
                    }
                }
                // The very first bandit ever encountered explains itself
                // before the fight actually starts — every later ambush
                // (this trap or any other) skips straight to combat.
                showBanditIntro -> ItemPreviewOverlay(
                    imageRes = R.drawable.ic_bandit_idle,
                    imageContentDescription = stringResource(R.string.good_samaritan_bandit_content_description),
                    title = stringResource(R.string.good_samaritan_bandit_preview_title),
                    description = stringResource(R.string.good_samaritan_bandit_preview_description),
                    onDismiss = onBanditPreviewAcknowledged,
                )
                combatOverlayVisible -> BanditPartyBattleOverlay(
                    combat = combat,
                    supplyCount = dungeonState.supplyCount,
                    characterCustomization = characterCustomization,
                    lastOutcome = dungeonState.lastOutcome,
                    onSupplyThrown = onSupplyThrown,
                    onSamaritanAttack = onSamaritanAttack,
                    onBanditAttack = onBanditAttack,
                    onRetreat = onRetreat,
                    onFinished = { combatOverlayVisible = false },
                )
                // The very first supply the player ever collects explains
                // itself right after being picked up.
                showSupplyIntro -> ItemPreviewOverlay(
                    imageRes = R.drawable.ic_medicine,
                    imageContentDescription = stringResource(R.string.good_samaritan_supply_content_description),
                    title = stringResource(R.string.good_samaritan_supply_preview_title),
                    description = stringResource(R.string.good_samaritan_supply_preview_description),
                    onDismiss = onMedicalSupplyPreviewAcknowledged,
                )
                helpingBeatOverlayShowing -> HelpingBeatOverlay(onDismiss = onHelpingBeatAcknowledged)
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
    onMapItemTapped: (MapItemPreview) -> Unit,
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

            // The real road-from-Jerusalem-to-Jericho map art, panned under
            // this fixed-size viewport by the same camera math used above —
            // one big image instead of tiling `ic_wall_rock` per wall cell,
            // since this source art is one cohesive illustration, not
            // tileable sprites (same call already made for Daniel's Race to
            // the Den maze background). `dungeonState.walls`' own boolean
            // grid is still the sole source of truth for collision; this is
            // rendering only.
            //
            // Drawn as a cropped-and-scaled region of the source bitmap via
            // a single `drawImage` call — deliberately *not* a plain `Image`
            // sized to the map's full `cols * cellSize` extent. With this
            // map's 56x30 grid, that naive approach asks Compose to lay out
            // and rasterize one bitmap tens of thousands of pixels wide
            // (`cellSize` is calibrated so `VIEWPORT_CELLS` fill the screen,
            // so scaling it up to the map's full 56-column width is enormous
            // regardless of the exact `VIEWPORT_CELLS` value), which
            // silently fails to draw at all on real hardware (GPU
            // texture/canvas size limits) — confirmed on-device as exactly
            // why the map didn't appear at all on the first pass.
            // `drawImage`'s `srcOffset`/`srcSize` crop the source bitmap
            // directly in its own pixel space instead, so the bitmap that
            // actually gets allocated/rasterized never grows past its real
            // 2816x1536 size regardless of how large the logical map grid is.
            val roadMapBitmap = ImageBitmap.imageResource(R.drawable.bg_good_samaritan_road_map)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val srcCellWidth = roadMapBitmap.width / dungeonState.cols.toFloat()
                val srcCellHeight = roadMapBitmap.height / dungeonState.rows.toFloat()
                val srcOffset = IntOffset(
                    (originX * srcCellWidth).roundToInt().coerceIn(0, roadMapBitmap.width),
                    (originY * srcCellHeight).roundToInt().coerceIn(0, roadMapBitmap.height),
                )
                val srcSize = IntSize(
                    (VIEWPORT_CELLS * srcCellWidth).roundToInt().coerceAtMost(roadMapBitmap.width - srcOffset.x),
                    (VIEWPORT_CELLS * srcCellHeight).roundToInt().coerceAtMost(roadMapBitmap.height - srcOffset.y),
                )
                drawImage(
                    image = roadMapBitmap,
                    srcOffset = srcOffset,
                    srcSize = srcSize,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                )
            }

            // Sized larger than a single cell on purpose (unlike the
            // collision radius, which stays PLAYER_RADIUS regardless) so the
            // character reads clearly at this map's zoom level — purely a
            // visual choice, doesn't change where the player can actually
            // walk. Tunable on-device. Declared here (not just below, next
            // to the character itself) since the bandit icon now shares
            // this same size.
            val characterSize = cellSize * 2f
            val supplyContentDescription = stringResource(R.string.good_samaritan_supply_content_description)
            val banditContentDescription = stringResource(R.string.good_samaritan_bandit_content_description)

            dungeonState.supplies.forEach { supply ->
                if (supply.id !in dungeonState.collectedSupplyIds) {
                    // Doubled from its original 0.6-cell size for better
                    // visibility at this map's zoom level, and to match the
                    // real recorded bandage art now used here instead of a
                    // small generic vector icon.
                    CenteredCellIcon(
                        drawableRes = R.drawable.ic_medicine,
                        position = supply.position,
                        cellSize = cellSize,
                        iconSize = cellSize * 1.2f,
                        originX = originX,
                        originY = originY,
                        contentDescription = supplyContentDescription,
                        onClick = { onMapItemTapped(MapItemPreview.SUPPLY) },
                    )
                }
            }

            dungeonState.traps.forEach { trap ->
                if (trap.id !in dungeonState.resolvedTrapIds) {
                    // The same bandit art as the turn-based fight overlay
                    // (BanditPartyBattleOverlay's idle pose), sized to match the
                    // player's own character — replaces the old generic
                    // ic_wall_bandit placeholder now that real bandit art
                    // exists.
                    CenteredCellIcon(
                        drawableRes = R.drawable.ic_bandit_idle,
                        position = trap.position,
                        cellSize = cellSize,
                        iconSize = characterSize,
                        originX = originX,
                        originY = originY,
                        contentDescription = banditContentDescription,
                        onClick = { onMapItemTapped(MapItemPreview.BANDIT) },
                    )
                }
            }

            if (!dungeonState.checkpointActivated) {
                // Grown from its original 0.7-cell size (first to 1.4x, then
                // further here) for better visibility at this map's zoom
                // level, same reasoning as the character/bandit sizing
                // above. Tunable on-device.
                CenteredCellIcon(
                    drawableRes = R.drawable.ic_traveler_injured,
                    position = dungeonState.checkpointPosition,
                    cellSize = cellSize,
                    iconSize = cellSize * 1.8f,
                    originX = originX,
                    originY = originY,
                    contentDescription = stringResource(R.string.good_samaritan_traveler_content_description),
                    // A tap here always explains the traveler generically —
                    // actually *reaching* him instead tells the parable's
                    // own next beat via HelpingBeatOverlay (see
                    // GoodSamaritanExploreContent's own overlay priority).
                    onClick = { onMapItemTapped(MapItemPreview.TRAVELER) },
                )
            }

            // No separate goal marker here — the map art already draws
            // "THE INN" building at the goal position, so a second generic
            // Inn icon on top would be redundant (mirrors dropping Daniel's
            // own goal marker once his maze art started showing the den
            // itself).

            // The player's own customized character, not a generic marker —
            // CharacterPreview applies `modifier.size(160.dp)` to whatever
            // modifier it's given, so a smaller `.size(...)` supplied here
            // constrains it first and the internal 160dp request is
            // clamped down to fit (same trick CharacterCallout already
            // relies on to show it at 96dp).
            // Anchored near the feet, not the sprite's vertical center — the
            // art has real headroom above the body, so centering it made the
            // character visually sink below the actual walkable path,
            // especially at this larger size (same fix already applied to
            // Daniel's Race to the Den maze). X stays centered; only Y is
            // pulled down toward the feet.
            val characterFeetAnchorFraction = 0.85f
            CharacterPreview(
                customization = characterCustomization,
                posture = Posture.STANDING,
                modifier = Modifier
                    .offset(
                        x = cellSize * (dungeonState.playerPosition.x - originX) - characterSize / 2,
                        y = cellSize * (dungeonState.playerPosition.y - originY) - characterSize * characterFeetAnchorFraction,
                    )
                    .size(characterSize)
                    .semantics { contentDescription = playerContentDescription },
            )
        }
    }
}

/**
 * [onClick], when supplied, shows [ItemPreviewOverlay]'s explainer for this
 * object on demand (see [MapItemPreview]) — the tap target grows to at
 * least [MIN_TAP_TARGET_SIZE] around the icon's own visual bounds so a
 * small icon (e.g. the medical supply, well under 48dp even doubled at this
 * map's zoom level) still meets this app's minimum touch-target size,
 * without changing where the icon itself is drawn or how big it looks.
 */
@Composable
private fun CenteredCellIcon(
    drawableRes: Int,
    position: Vector2,
    cellSize: Dp,
    iconSize: Dp,
    originX: Float,
    originY: Float,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val tapTargetSize = if (onClick != null) maxOf(iconSize, MIN_TAP_TARGET_SIZE) else iconSize
    var tapTargetModifier = Modifier
        .offset(
            x = cellSize * (position.x - originX) - tapTargetSize / 2,
            y = cellSize * (position.y - originY) - tapTargetSize / 2,
        )
        .size(tapTargetSize)
    if (onClick != null) {
        val tapDescription = contentDescription.orEmpty()
        tapTargetModifier = tapTargetModifier
            .clickable(onClickLabel = tapDescription, onClick = onClick)
            .semantics(mergeDescendants = true) { this.contentDescription = tapDescription }
    }
    Box(modifier = tapTargetModifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * A tap-or-first-encounter explainer for a map object (medical supplies,
 * bandits, or the injured traveler when *tapped* rather than actually
 * reached) — same scrim+card shape as [HelpingBeatOverlay], just generic
 * over which image/title/description to show. Consumes all touches like
 * every other overlay in this screen, so the joystick underneath can't be
 * dragged while it's up.
 */
@Composable
private fun ItemPreviewOverlay(
    imageRes: Int,
    imageContentDescription: String,
    title: String,
    description: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = imageContentDescription,
                    modifier = Modifier.padding(top = 12.dp).size(120.dp),
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                )
                AdventureMenuButton(text = stringResource(R.string.action_continue), onClick = onDismiss)
            }
        }
    }
}

// BanditPartyBattleOverlay — the turn-based fight itself — now lives in
// GoodSamaritanBanditBattleOverlay.kt in this same package, alongside the
// combat-only animation constants it needs.

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
                Image(
                    painter = painterResource(R.drawable.ic_traveler_injured),
                    contentDescription = stringResource(R.string.good_samaritan_traveler_content_description),
                    modifier = Modifier.padding(top = 12.dp).size(120.dp),
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
            medicalSupplyPreviewAcknowledged = false,
            banditPreviewAcknowledged = false,
            onDungeonTick = { _, _ -> },
            onSupplyThrown = {},
            onSamaritanAttack = {},
            onBanditAttack = {},
            onRetreat = {},
            onHelpingBeatAcknowledged = {},
            onMedicalSupplyPreviewAcknowledged = {},
            onBanditPreviewAcknowledged = {},
            onContinue = {},
            onBackToMainMenu = {},
        )
    }
}
