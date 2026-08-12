package com.bibleadventures.game.puzzles.groupfill

/** One family/group with a headcount to be routed into a seating circle. */
data class FamilyGroup(val id: String, val headcount: Int)

/** Never FAILED — a drop that would overshoot a circle's target is simply rejected, no progress lost. */
enum class GroupFillOutcome { NONE, ADDED, REJECTED_OVERSHOOT, CIRCLE_COMPLETE, ALL_COMPLETE }

/**
 * Drag [families] into one of several seating circles, each with an exact
 * numeric [circleTargets] to reach — unlike [com.bibleadventures.game.puzzles.dragsort.DragSortGameState]
 * (static category match) or [com.bibleadventures.game.puzzles.stackbuild.StackBuildGameState]
 * (strict order), correctness here depends on a *running sum*: a family
 * fits a circle only if it doesn't push that circle's total past its
 * target. Whether a drop lands on a given circle at all is screen-side
 * geometry (same split as every other drag engine in this app); this
 * engine only judges whether the drop fits.
 */
data class GroupFillGameState(
    val families: List<FamilyGroup>,
    val circleTargets: List<Int>,
    val circleContents: List<List<String>> = List(circleTargets.size) { emptyList() },
    val lastOutcome: GroupFillOutcome = GroupFillOutcome.NONE,
) {
    private fun sumOf(familyIds: List<String>): Int = familyIds.sumOf { id -> families.first { it.id == id }.headcount }

    fun circleSum(circleIndex: Int): Int = sumOf(circleContents[circleIndex])
    fun isCircleComplete(circleIndex: Int): Boolean = circleSum(circleIndex) == circleTargets[circleIndex]

    val placedFamilyIds: Set<String> get() = circleContents.flatten().toSet()
    val remainingFamilyIds: List<String> get() = families.map { it.id }.filterNot { it in placedFamilyIds }
    val isComplete: Boolean get() = circleTargets.indices.all(::isCircleComplete)
}
