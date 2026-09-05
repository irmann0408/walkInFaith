package com.bibleadventures.ui.screens.goodsamaritan.explore

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bibleadventures.R
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.game.puzzles.dungeon.DungeonCombatState
import com.bibleadventures.game.puzzles.dungeon.DungeonGame
import com.bibleadventures.game.puzzles.dungeon.DungeonOutcome
import com.bibleadventures.ui.components.AdventureMenuButton
import com.bibleadventures.ui.components.CharacterPreview
import kotlinx.coroutines.delay

private const val PROJECTILE_FLIGHT_DURATION_MS = 350

/** Peak height (above the straight-line path) of the thrown supply's arc — a lob, not a flat slide. Tunable on-device. */
private val THROW_ARC_HEIGHT = 48.dp

/** How long a melee lunge (the Samaritan's own strike, or the bandit's counter) takes, out and back — shared by both, since they're now the same kind of animation. */
private const val LUNGE_OUT_DURATION_MS = 250
private const val LUNGE_BACK_DURATION_MS = 250

/** How far across the gap (as a fraction of the distance to the target) a melee lunge travels — a leap, not a full swap of places. */
private const val LUNGE_FRACTION = 0.55f

/** How long an attack's result stays visible, at the peak of its lunge/throw, before the actor settles back to idle. */
private const val ATTACK_RESULT_HOLD_MS = 500L

/** A brief pause before each automatic turn starts, so the turn-indicator text change is actually noticeable rather than snapping straight from one actor's animation into the next. */
private const val TURN_TRANSITION_DELAY_MS = 300L

/** How long the final, defeating hit's result stays on screen before the overlay actually closes — long enough that the killing blow is never skipped past. */
private const val BANDIT_DEFEATED_HOLD_MS = 500L

/** The bandit's own size — also the baseline [SAMARITAN_SPRITE_SIZE] scales from. */
private val COMBAT_SPRITE_SIZE = 140.dp

/** Smaller than the bandit/Samaritan — CharacterPreview's own full-body art reads noticeably larger than either at an equal bounding-box size, so this deliberately undersizes the box to match. */
private val PLAYER_SPRITE_SIZE = 100.dp

/** Bigger than the bandit/player, not smaller — a real party member and an adult on a donkey, not a sidekick. Tunable on-device. */
private val SAMARITAN_SPRITE_SIZE = COMBAT_SPRITE_SIZE * 1.25f

private enum class BattleActorPose { IDLE, ATTACKING }

/** Whose turn is currently animating — purely a screen-local sequencing concern (see this file's own top doc comment for why [com.bibleadventures.game.puzzles.dungeon.DungeonGameState] itself needs no matching field). */
private enum class BattleTurn { PLAYER, SAMARITAN, BANDIT }

/**
 * A confirmed, explicit exception to this app's normal "no combat / no
 * failure states" rule (see `docs/PROJECT_STATUS.md`'s Good Samaritan
 * dungeon addendum) — kept as gentle as the app's only other exception
 * (David & Goliath's Connect Four): neither the bandit's nor (obviously)
 * the Samaritan's attack ever hurts the player, only the bandit's own
 * counter risks a stolen supply, and running out of supplies never ends
 * the run — Retreat leaves the bandit for later with nothing lost but the
 * supplies already spent.
 *
 * A real party turn order, not a ping-pong (this codebase's only prior
 * turn-based precedent, David & Goliath's Connect Four, is strictly
 * 2-actor): each round is player → Samaritan → bandit, always in that
 * order, driven end to end by a single [LaunchedEffect] once the player
 * taps to start it — see [DungeonGame.onSamaritanAttack]'s own doc comment
 * for why the Samaritan's turn costs no supply. A defeating hit at either
 * of the party's two turns skips the rest of the round and resolves the
 * fight immediately, exactly like a defeating throw always has.
 *
 * The player's own throw still commits to game state *immediately* on tap
 * (unlike Sling Practice's deferred-until-animation-lands pattern, which
 * exists there because the outcome depends on a *moving* target's future
 * position — here every roll is already resolved by the time any
 * animation starts); the Samaritan's and bandit's own turns are each
 * triggered automatically by this composable, not tapped.
 *
 * [combat] is nullable specifically so a defeating attack still gets to
 * visually land: the engine clears `combat` the instant a hit resolves,
 * but this overlay needs to keep rendering (using [displayedCombat], its
 * own last-known-good snapshot) until its own animation sequence
 * finishes, then calls [onFinished] to tell the parent it's safe to
 * actually unmount.
 */
@Composable
internal fun BanditPartyBattleOverlay(
    combat: DungeonCombatState?,
    supplyCount: Int,
    characterCustomization: CharacterCustomization,
    lastOutcome: DungeonOutcome,
    onSupplyThrown: () -> Unit,
    onSamaritanAttack: () -> Unit,
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

    var roundTrigger by remember { mutableIntStateOf(0) }
    var isResolving by remember { mutableStateOf(false) }
    var currentTurn by remember { mutableStateOf(BattleTurn.PLAYER) }
    var samaritanPose by remember { mutableStateOf(BattleActorPose.IDLE) }
    var banditPose by remember { mutableStateOf(BattleActorPose.IDLE) }
    // Which party member the bandit's own counter-attack visually lunges
    // at — alternated each time so it reads as landing on someone (the
    // mechanic itself is always "the party," never one character
    // specifically; this is purely which sprite the lunge animates toward).
    var banditTargetsPlayer by remember { mutableStateOf(true) }
    val flightProgress = remember { Animatable(1f) }
    // 0 = at its home spot, 1 = fully lunged toward its target — driven out
    // and back around each attack's own roll, not a static pose swap.
    val samaritanLungeProgress = remember { Animatable(0f) }
    val banditLungeProgress = remember { Animatable(0f) }
    val latestCombat by rememberUpdatedState(combat)

    LaunchedEffect(roundTrigger) {
        if (roundTrigger == 0) return@LaunchedEffect
        isResolving = true

        // Player's turn: the throw was already rolled at tap time (see
        // onSupplyThrown's own call site below) — this just plays it out.
        currentTurn = BattleTurn.PLAYER
        flightProgress.snapTo(0f)
        flightProgress.animateTo(1f, animationSpec = tween(PROJECTILE_FLIGHT_DURATION_MS))

        if (latestCombat == null) {
            delay(BANDIT_DEFEATED_HOLD_MS)
            onFinished()
            return@LaunchedEffect
        }

        // The Samaritan's turn: automatic, no tap, no supply spent.
        currentTurn = BattleTurn.SAMARITAN
        delay(TURN_TRANSITION_DELAY_MS)
        samaritanPose = BattleActorPose.ATTACKING
        samaritanLungeProgress.animateTo(1f, animationSpec = tween(LUNGE_OUT_DURATION_MS))
        onSamaritanAttack()
        delay(ATTACK_RESULT_HOLD_MS)
        samaritanLungeProgress.animateTo(0f, animationSpec = tween(LUNGE_BACK_DURATION_MS))
        samaritanPose = BattleActorPose.IDLE

        if (latestCombat == null) {
            delay(BANDIT_DEFEATED_HOLD_MS)
            onFinished()
            return@LaunchedEffect
        }

        // The bandit's own turn: automatic counter-attack, unchanged mechanic.
        currentTurn = BattleTurn.BANDIT
        delay(TURN_TRANSITION_DELAY_MS)
        banditTargetsPlayer = !banditTargetsPlayer
        banditPose = BattleActorPose.ATTACKING
        banditLungeProgress.animateTo(1f, animationSpec = tween(LUNGE_OUT_DURATION_MS))
        onBanditAttack()
        delay(ATTACK_RESULT_HOLD_MS)
        banditLungeProgress.animateTo(0f, animationSpec = tween(LUNGE_BACK_DURATION_MS))
        banditPose = BattleActorPose.IDLE

        currentTurn = BattleTurn.PLAYER
        isResolving = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.good_samaritan_bandit_encounter_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = when (currentTurn) {
                    BattleTurn.PLAYER -> stringResource(R.string.good_samaritan_battle_your_turn)
                    BattleTurn.SAMARITAN -> stringResource(R.string.good_samaritan_battle_samaritan_turn)
                    BattleTurn.BANDIT -> stringResource(R.string.good_samaritan_battle_bandit_turn)
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
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
                DungeonOutcome.SAMARITAN_HIT -> stringResource(R.string.dungeon_feedback_samaritan_hit)
                DungeonOutcome.SAMARITAN_ATTACK_MISSED -> stringResource(R.string.dungeon_feedback_samaritan_attack_missed)
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

            // The party's own column (player up top, the Samaritan well
            // below him — same X, different rows, so there's real vertical
            // room between them) vs. the bandit's column, far off to the
            // right at roughly the row between the two of them. Two
            // *different* columns, not just "grouped but still side by
            // side": an earlier layout kept player and Samaritan side by
            // side on one row, and the Samaritan's own lunge toward the
            // bandit still had to sweep across in front of the player to
            // get there, reading as an attack on the player instead.
            // Stacking them in one shared column instead means every
            // attack's own path — the player's thrown arc, the Samaritan's
            // lunge, the bandit's counter-lunge — can aim at its real
            // target's actual position without ever crossing back over an
            // ally standing in the way.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f, fill = true).padding(top = 16.dp)) {
                val partyColumnCenterX = maxOf(PLAYER_SPRITE_SIZE, SAMARITAN_SPRITE_SIZE) / 2
                val banditColumnCenterX = maxWidth - COMBAT_SPRITE_SIZE / 2

                // A real but moderate gap between the two rows — not
                // pinned to the box's own top/bottom edges, which read as
                // too far apart once this overlay had a whole screen's
                // height to stretch across instead of a small card.
                val playerHomeCenterY = maxHeight * 0.25f
                val samaritanHomeCenterY = maxHeight * 0.68f
                // The bandit's own home row sits between the two, but its
                // *lunge* always aims at one specific party member's real
                // position (see banditTargetsPlayer) — a home row exactly
                // between them was previously also the lunge's own target,
                // landing on neither and reading as swinging at empty air.
                val banditHomeCenterY = (playerHomeCenterY + samaritanHomeCenterY) / 2

                // Both melee lunges — the Samaritan's own strike and the
                // bandit's counter — travel diagonally, straight at the
                // other's real (x, y) position, not just sideways along
                // one shared row.
                val samaritanLungeCenterX = partyColumnCenterX + (banditColumnCenterX - partyColumnCenterX) * LUNGE_FRACTION
                val samaritanLungeCenterY = samaritanHomeCenterY + (banditHomeCenterY - samaritanHomeCenterY) * LUNGE_FRACTION
                val samaritanCenterX = partyColumnCenterX + (samaritanLungeCenterX - partyColumnCenterX) * samaritanLungeProgress.value
                val samaritanCenterY = samaritanHomeCenterY + (samaritanLungeCenterY - samaritanHomeCenterY) * samaritanLungeProgress.value

                val banditLungeTargetY = if (banditTargetsPlayer) playerHomeCenterY else samaritanHomeCenterY
                val banditLungeCenterX = banditColumnCenterX - (banditColumnCenterX - partyColumnCenterX) * LUNGE_FRACTION
                val banditLungeCenterY = banditHomeCenterY + (banditLungeTargetY - banditHomeCenterY) * LUNGE_FRACTION
                val banditCenterX = banditColumnCenterX + (banditLungeCenterX - banditColumnCenterX) * banditLungeProgress.value
                val banditCenterY = banditHomeCenterY + (banditLungeCenterY - banditHomeCenterY) * banditLungeProgress.value

                val samaritanContentDescription = stringResource(R.string.good_samaritan_samaritan_content_description)
                Image(
                    painter = painterResource(if (samaritanPose == BattleActorPose.ATTACKING) R.drawable.ic_good_samaritan_attack else R.drawable.ic_good_samaritan),
                    contentDescription = samaritanContentDescription,
                    modifier = Modifier
                        .offset(x = samaritanCenterX - SAMARITAN_SPRITE_SIZE / 2, y = samaritanCenterY - SAMARITAN_SPRITE_SIZE / 2)
                        .size(SAMARITAN_SPRITE_SIZE),
                )

                Image(
                    painter = painterResource(if (banditPose == BattleActorPose.ATTACKING) R.drawable.ic_bandit_attack else R.drawable.ic_bandit_idle),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = banditCenterX - COMBAT_SPRITE_SIZE / 2, y = banditCenterY - COMBAT_SPRITE_SIZE / 2)
                        .size(COMBAT_SPRITE_SIZE),
                )

                if (supplyCount > 0) {
                    val throwDescription = stringResource(R.string.good_samaritan_throw_supply_content_description)
                    CharacterPreview(
                        customization = characterCustomization,
                        modifier = Modifier
                            .offset(x = partyColumnCenterX - PLAYER_SPRITE_SIZE / 2, y = playerHomeCenterY - PLAYER_SPRITE_SIZE / 2)
                            .size(PLAYER_SPRITE_SIZE)
                            .clickable(enabled = !isResolving, onClickLabel = throwDescription) {
                                roundTrigger++
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
                    // A real diagonal arc from the player's own row down (or
                    // up) to the bandit's row, not a flat slide at a fixed
                    // height — the two are no longer on the same row now
                    // that the party is stacked in one column.
                    val projectileX = partyColumnCenterX + (banditColumnCenterX - partyColumnCenterX) * progress
                    val projectileBaseY = playerHomeCenterY + (banditHomeCenterY - playerHomeCenterY) * progress
                    // A thrown-underhand lob, not a flat straight-line
                    // slide: a parabola peaking at the midpoint
                    // (progress = 0.5) and back to baseline at both ends,
                    // scaled by THROW_ARC_HEIGHT.
                    val arcLift = THROW_ARC_HEIGHT * 4f * progress * (1f - progress)
                    Image(
                        painter = painterResource(R.drawable.ic_medicine),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = projectileX - 16.dp, y = projectileBaseY - 16.dp - arcLift)
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
