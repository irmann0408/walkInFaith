package com.bibleadventures.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AdventureProgress(
    val chapterId: ChapterId,
    val completed: Boolean = false,
    val stars: Int = 0,
    val completedActivities: Set<String> = emptySet(),
)
