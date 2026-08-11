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
            GridTileType.COLLECTIBLE -> {
                if (next !in state.collectedPositions) {
                    nextState = nextState.copy(
                        collectedPositions = state.collectedPositions + next,
                        lastOutcome = GridMazeOutcome.COLLECTED,
                    )
                }
            }
            GridTileType.CHECKPOINT -> {
                if (!state.checkpointActivated) {
                    nextState = if (state.hasCollectible) {
                        nextState.copy(checkpointActivated = true, lastOutcome = GridMazeOutcome.CHECKPOINT_ACTIVATED)
                    } else {
                        nextState.copy(lastOutcome = GridMazeOutcome.CHECKPOINT_NEEDS_COLLECTIBLE)
                    }
                }
            }
            else -> Unit
        }

        return nextState
    }
}
