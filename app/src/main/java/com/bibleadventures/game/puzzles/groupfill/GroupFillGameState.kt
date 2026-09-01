package com.bibleadventures.game.puzzles.groupfill

/** One family/group with a headcount to be routed into a seating circle. */
data class FamilyGroup(val id: String, val headcount: Int)

/** Never FAILED — a drop that would overshoot a circle's target, or strand another circle unreachable, is simply rejected, no progress lost. */
enum class GroupFillOutcome { NONE, ADDED, REJECTED_OVERSHOOT, REJECTED_UNREACHABLE, CIRCLE_COMPLETE, ALL_COMPLETE }

/**
 * Drag [families] into one of several seating circles, each with an exact
 * numeric [circleTargets] to reach — unlike a static category match
 * (any of several fixed, order-independent bins with no numeric target) or
 * [com.bibleadventures.game.puzzles.stackbuild.StackBuildGameState]
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

    /**
     * Whether every incomplete circle's remaining gap can still be reached by
     * *some* subset of the remaining pool. Checked independently per circle
     * (not a joint disjoint-partition solve) — cheap, and in practice a
     * circle that's individually unreachable is exactly the "kid is stuck"
     * scenario this exists to prevent.
     */
    fun isEveryCircleStillReachable(): Boolean {
        val remainingHeadcounts = remainingFamilyIds.map { id -> families.first { it.id == id }.headcount }
        return circleTargets.indices.all { index -> isSubsetSumReachable(circleTargets[index] - circleSum(index), remainingHeadcounts) }
    }
}

/** Classic subset-sum reachability: can some subset of [numbers] sum exactly to [target]? */
private fun isSubsetSumReachable(target: Int, numbers: List<Int>): Boolean {
    if (target == 0) return true
    if (target < 0) return false
    val reachable = BooleanArray(target + 1)
    reachable[0] = true
    for (number in numbers) {
        for (sum in target downTo number) {
            if (reachable[sum - number]) reachable[sum] = true
        }
    }
    return reachable[target]
}
