package com.bibleadventures.game.puzzles.stealth

import com.bibleadventures.game.puzzles.gridmaze.Direction

/**
 * Turn-based courtyard stealth, reusing gridmaze's [com.bibleadventures.game.puzzles.gridmaze.GridPosition]/
 * [Direction]. A wall bump is free — no guard advance, mirroring gridmaze's
 * own wall-bump. A successful move advances every guard's patrol by one
 * step; if the player's new position falls inside the now-advanced
 * watched-cell set, they're SPOTTED and walked back to [StealthGameState.startPosition] —
 * guards keep their same deterministic cycle, nothing else is lost. Never a
 * hard fail.
 */
object StealthGame {

    fun onDirectionPressed(state: StealthGameState, direction: Direction): StealthGameState {
        if (state.isComplete) return state

        val next = when (direction) {
            Direction.UP -> state.playerPosition.copy(row = state.playerPosition.row - 1)
            Direction.DOWN -> state.playerPosition.copy(row = state.playerPosition.row + 1)
            Direction.LEFT -> state.playerPosition.copy(col = state.playerPosition.col - 1)
            Direction.RIGHT -> state.playerPosition.copy(col = state.playerPosition.col + 1)
        }

        val inBounds = next.row in state.grid.indices && next.col in state.grid[next.row].indices
        if (!inBounds || state.grid[next.row][next.col] == StealthTileType.WALL) {
            return state.copy(lastOutcome = StealthOutcome.BLOCKED)
        }

        val advancedTurn = state.turnIndex + 1
        val watchedAfterMove = state.guards
            .flatMap { guard -> guard.patrol[advancedTurn % guard.patrol.size].watchedCells }
            .toSet()

        if (next in watchedAfterMove) {
            return state.copy(
                playerPosition = state.startPosition,
                turnIndex = advancedTurn,
                lastOutcome = StealthOutcome.SPOTTED,
            )
        }

        val outcome = if (state.grid[next.row][next.col] == StealthTileType.GOAL) {
            StealthOutcome.COMPLETE
        } else {
            StealthOutcome.MOVED
        }
        return state.copy(playerPosition = next, turnIndex = advancedTurn, lastOutcome = outcome)
    }
}
