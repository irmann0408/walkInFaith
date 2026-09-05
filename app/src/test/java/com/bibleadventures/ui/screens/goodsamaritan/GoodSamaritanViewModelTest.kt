package com.bibleadventures.ui.screens.goodsamaritan

import com.bibleadventures.FakeAudioController
import com.bibleadventures.FakePlayerProfileRepository
import com.bibleadventures.MainDispatcherRule
import com.bibleadventures.audio.SoundEffect
import com.bibleadventures.domain.model.ChapterId
import com.bibleadventures.game.puzzles.dungeon.DungeonGame
import com.bibleadventures.game.puzzles.dungeon.DungeonOutcome
import com.bibleadventures.game.puzzles.dungeon.Vector2
import com.bibleadventures.game.puzzles.roadblock.RoadblockOutcome
import com.bibleadventures.game.stories.GoodSamaritanContent
import com.bibleadventures.progress.ProgressionService
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoodSamaritanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** A [Random] whose `nextFloat()` always returns [value] — see `DungeonGameTest`'s own identical helper for why a fixed-return fake beats a seeded sequence here. */
    private fun fixedRandom(value: Float): Random = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextFloat(): Float = value
    }

    /**
     * Defaults to a guaranteed-success roll: none of these tests are about
     * [DungeonGame.PLAYER_HIT_CHANCE]/[DungeonGame.BANDIT_STEAL_CHANCE]'s
     * own randomness (that's `DungeonGameTest`'s job) — using
     * `Random.Default` here would make any test that throws a supply or
     * triggers a bandit attack flaky, since a real roll could occasionally
     * miss/steal unexpectedly. Tests that specifically want a miss pass
     * their own [fixedRandom] value.
     */
    private fun createViewModel(
        repository: FakePlayerProfileRepository = FakePlayerProfileRepository(),
        audioController: FakeAudioController = FakeAudioController(),
        random: Random = fixedRandom(0f),
    ) = GoodSamaritanViewModel(ProgressionService(repository), repository, audioController, random)

    /**
     * Walks the real, hand-verified production route
     * ([GoodSamaritanContent.dungeonRouteWaypoints]) leg by leg until a
     * bandit encounter triggers — since bandits now patrol (see
     * [GoodSamaritanContent.banditPatrols]) rather than sitting still, an
     * encounter is a matter of timing, not just position, so this can't
     * hand-pick a short prefix of turns the way a stationary-bandit map
     * could; it has to walk the same route [DungeonGameTest]'s own replay
     * test already verified converges on a fight partway through. Uses the
     * default [createViewModel] random (a guaranteed-success roll), the
     * same worst-case assumption that route was verified against, so
     * there's always enough banked supply by the time the fight starts.
     * Shared by every combat-related test below.
     */
    private fun walkToFirstBanditEncounter(viewModel: GoodSamaritanViewModel) {
        for (turn in GoodSamaritanContent.dungeonRouteWaypoints) {
            if (viewModel.uiState.value.dungeonState.combat != null) break
            walkToward(viewModel, target = turn)
        }
        check(viewModel.uiState.value.dungeonState.combat != null) { "Expected a bandit encounter to have triggered by now" }
    }

    @Test
    fun `initial dungeonState parses mapLayout into the correct dimensions and landmark positions`() {
        val state = createViewModel().uiState.value.dungeonState

        assertEquals(GoodSamaritanContent.mapLayout.size, state.rows)
        assertEquals(GoodSamaritanContent.mapLayout[0].length, state.cols)
        assertEquals(Vector2(9.5f, 8.5f), state.playerPosition)
        assertEquals(Vector2(49.5f, 15.5f), state.checkpointPosition)
        assertEquals(Vector2(46.5f, 24.5f), state.goalPosition)
        assertEquals(7, state.supplies.size)
    }

    @Test
    fun `onDungeonTick plays a sound on collecting a supply, not on an ordinary movement-only frame`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)

        // A tiny nudge, nowhere near anything — no sound.
        viewModel.onDungeonTick(Vector2(1f, 0f), deltaSeconds = 0.01f)
        assertTrue(audioController.playedEffects.isEmpty())

        // Walk the real route's opening legs until the first supply pickup.
        for (turn in GoodSamaritanContent.dungeonRouteWaypoints) {
            if (viewModel.uiState.value.dungeonState.collectedSupplyIds.isNotEmpty()) break
            walkToward(viewModel, target = turn)
        }

        assertEquals(listOf(SoundEffect.ITEM_COLLECTED), audioController.playedEffects)
    }

    @Test
    fun `onSupplyThrown plays a hit sound, then a scared-off sound once the bandit's toughness reaches zero`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController)
        walkToFirstBanditEncounter(viewModel)
        check(viewModel.uiState.value.dungeonState.supplyCount >= DungeonGame.BANDIT_INITIAL_TOUGHNESS) {
            "Expected at least ${DungeonGame.BANDIT_INITIAL_TOUGHNESS} supplies before the fight, had ${viewModel.uiState.value.dungeonState.supplyCount}"
        }

        repeat(DungeonGame.BANDIT_INITIAL_TOUGHNESS - 1) { viewModel.onSupplyThrown() }
        assertEquals(SoundEffect.TARGET_HIT, audioController.playedEffects.last())

        viewModel.onSupplyThrown()
        assertEquals(SoundEffect.OBSTACLE_DODGED, audioController.playedEffects.last())
        assertNull(viewModel.uiState.value.dungeonState.combat)
    }

    @Test
    fun `onBanditAttack plays a dodge sound and changes nothing else on an unfavorable roll`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController, random = fixedRandom(0.999f))
        walkToFirstBanditEncounter(viewModel)
        val supplyCountBefore = viewModel.uiState.value.dungeonState.supplyCount

        viewModel.onBanditAttack()

        assertEquals(DungeonOutcome.BANDIT_ATTACK_MISSED, viewModel.uiState.value.dungeonState.lastOutcome)
        assertEquals(supplyCountBefore, viewModel.uiState.value.dungeonState.supplyCount)
        assertEquals(SoundEffect.OBSTACLE_DODGED, audioController.playedEffects.last())
    }

    @Test
    fun `onBanditAttack steals a supply silently on a favorable roll`() {
        val audioController = FakeAudioController()
        val viewModel = createViewModel(audioController = audioController, random = fixedRandom(0f))
        walkToFirstBanditEncounter(viewModel)
        val supplyCountBefore = viewModel.uiState.value.dungeonState.supplyCount

        viewModel.onBanditAttack()

        assertEquals(DungeonOutcome.SUPPLY_STOLEN, viewModel.uiState.value.dungeonState.lastOutcome)
        assertEquals(supplyCountBefore - 1, viewModel.uiState.value.dungeonState.supplyCount)
        assertTrue("stealing a supply isn't a sound cue this app plays", SoundEffect.OBSTACLE_DODGED !in audioController.playedEffects)
    }

    /**
     * Steers straight at [target] one small frame at a time through the
     * real ViewModel API — mirrors `DungeonGameTest`'s own `steerToward`
     * (see its doc comment for why the direction is normalized to a fixed
     * magnitude rather than the raw, shrinking distance-to-target vector),
     * just driven through [GoodSamaritanViewModel.onDungeonTick] instead of
     * calling the pure engine directly, since this test is about the
     * ViewModel's wiring, not the engine's own movement math (already
     * covered by DungeonGameTest).
     */
    private fun walkToward(viewModel: GoodSamaritanViewModel, target: Vector2, maxSteps: Int = 2_000) {
        repeat(maxSteps) {
            val current = viewModel.uiState.value.dungeonState
            if (current.combat != null || current.playerPosition.distanceTo(target) <= 0.05f) return
            val dx = target.x - current.playerPosition.x
            val dy = target.y - current.playerPosition.y
            val magnitude = kotlin.math.hypot(dx, dy)
            viewModel.onDungeonTick(Vector2(dx / magnitude, dy / magnitude), deltaSeconds = 1f / 60f)
        }
    }

    @Test
    fun `initial roadblockState parses passingByLevels' first level into the correct dimensions and blocks`() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value.roadblockState
        val level1 = GoodSamaritanContent.passingByLevels[0]

        assertEquals(level1.layout.size, state.rows)
        assertEquals(level1.layout[0].length, state.cols)
        assertEquals(level1.blockSpecs.size, state.blocks.size)
        assertEquals(GoodSamaritanContent.passingByProtagonistId, state.protagonistId)
        assertEquals(level1.exitColumns, state.exitColumns)
        assertTrue(state.blocks.first { it.id == "injured_man" }.isFixed)
        assertEquals(0, viewModel.uiState.value.passingByLevelIndex)
    }

    @Test
    fun `onSlideAttempted delegates to RoadblockGame and updates uiState`() {
        val viewModel = createViewModel()

        // "obstacle_1" sliding down is the first move of level 1's hand-verified solution.
        viewModel.onSlideAttempted("obstacle_1", com.bibleadventures.game.puzzles.roadblock.Direction.DOWN, 1)

        assertEquals(RoadblockOutcome.MOVED, viewModel.uiState.value.roadblockState.lastOutcome)
    }

    @Test
    fun `all 4 passingByLevels solved back to back advances the level index each time and never leaves the roadblock scene early`() {
        val viewModel = createViewModel()

        GoodSamaritanContent.passingByLevels.forEachIndexed { index, level ->
            level.solution.forEach { move -> viewModel.onSlideAttempted(move.blockId, move.direction, move.distance) }
            assertTrue(viewModel.uiState.value.roadblockState.isComplete)
            assertEquals(index, viewModel.uiState.value.passingByLevelIndex)

            viewModel.onPassingByNextLevel()
        }

        // onPassingByNextLevel is a no-op past the last level — there's no 5th level to advance into.
        assertEquals(GoodSamaritanContent.passingByLevels.lastIndex, viewModel.uiState.value.passingByLevelIndex)
        assertTrue("the final level's own board should stay solved, not reset", viewModel.uiState.value.roadblockState.isComplete)
    }

    @Test
    fun `onPassingByNextLevel is a no-op before the current level is actually solved`() {
        val viewModel = createViewModel()

        viewModel.onPassingByNextLevel()

        assertEquals(0, viewModel.uiState.value.passingByLevelIndex)
        assertFalse(viewModel.uiState.value.roadblockState.isComplete)
    }

    @Test
    fun `onHelpingBeatAcknowledged flips the flag`() {
        val viewModel = createViewModel()

        viewModel.onHelpingBeatAcknowledged()

        assertTrue(viewModel.uiState.value.helpingBeatAcknowledged)
    }

    @Test
    fun `onSceneCompleted marks the scene as a completed activity for the Good Samaritan`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onSceneCompleted("intro")
        advanceUntilIdle()

        val progress = repository.current().progressByChapter.getValue(ChapterId.GOOD_SAMARITAN)
        assertTrue("intro" in progress.completedActivities)
        assertFalse(progress.completed)
    }

    @Test
    fun `onChapterFinished awards full stars and completes the chapter exactly once`() = runTest {
        val repository = FakePlayerProfileRepository()
        val viewModel = createViewModel(repository = repository)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.reward)
        assertEquals(3, viewModel.uiState.value.reward?.stars)
        assertTrue(ChapterId.GOOD_SAMARITAN in repository.current().completedChapters)
        assertEquals(3, repository.current().stars)

        viewModel.onChapterFinished()
        advanceUntilIdle()

        assertEquals(3, repository.current().stars)

        job.cancel()
    }
}
