package com.bibleadventures.game.rewards

import org.junit.Assert.assertEquals
import org.junit.Test

class RewardCalculatorTest {

    @Test
    fun `completing the chapter awards full stars`() {
        assertEquals(3, RewardCalculator.calculateStars(chapterCompleted = true))
    }

    @Test
    fun `not completing the chapter awards no stars`() {
        assertEquals(0, RewardCalculator.calculateStars(chapterCompleted = false))
    }
}
