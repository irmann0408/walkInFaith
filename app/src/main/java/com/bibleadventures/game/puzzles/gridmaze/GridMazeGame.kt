package com.bibleadventures.game.puzzles.gridmaze

/**
 * Pure grid-movement transition logic — no Compose/Android dependency,
 * directly unit-testable. A wall or an out-of-bounds move is a neutral
 * no-op (BLOCKED), never a failure state: the player just doesn't move,
 * with no penalty, and can immediately try a different direction.
 */
object GridMazeGame {

    fun onDirectionPressed(state: GridMazeState, direction: Direction): GridMazeState {
        if (state.isComplete) return state

        val current = state.playerPosition
        val next = when (direction) {
            Direction.UP -> current.copy(row = current.row - 1)
            Direction.DOWN -> current.copy(row = current.row + 1)
            Direction.LEFT -> current.copy(col = current.col - 1)
            Direction.RIGHT -> current.copy(col = current.col + 1)
        }

        val rows = state.grid.size
        val cols = state.grid[0].size
        if (next.row !in 0 until rows || next.col !in 0 until cols) {
            return state.copy(lastOutcome = GridMazeOutcome.BLOCKED)
        }

        val nextTile = state.grid[next.row][next.col]
        if (nextTile == GridTileType.WALL) {
            return state.copy(lastOutcome = GridMazeOutcome.BLOCKED)
        }

        var nextState = state.copy(playerPosition = next, lastOutcome = GridMazeOutcome.MOVED)

        when (nextTile) {
            GridTileType.MEDICINE -> {
                if (next !in state.medicineCollected) {
                    nextState = nextState.copy(
                        medicineCollected = state.medicineCollected + next,
                        lastOutcome = GridMazeOutcome.MEDICINE_COLLECTED,
                    )
                }
            }
            GridTileType.TRAVELER -> {
                if (!state.travelerTreated) {
                    nextState = if (state.hasMedicine) {
                        nextState.copy(travelerTreated = true, lastOutcome = GridMazeOutcome.TRAVELER_TREATED)
                    } else {
                        nextState.copy(lastOutcome = GridMazeOutcome.TRAVELER_NEEDS_MEDICINE)
                    }
                }
            }
            else -> Unit
        }

        return nextState
    }
}
