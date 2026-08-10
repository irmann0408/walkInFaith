package com.bibleadventures.domain.model

import kotlinx.serialization.Serializable

/**
 * Stable identifier for each Bible adventure chapter. Never rename an
 * existing constant — the name is used as-is inside persisted save data.
 */
@Serializable
enum class ChapterId {
    NOAHS_ARK,
    DAVID_GOLIATH,
    GOOD_SAMARITAN,
    FEEDING_5000,
    DANIEL,
    JESUS_CALMS_STORM,
}
