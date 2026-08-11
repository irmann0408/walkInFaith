package com.bibleadventures.domain.model

import kotlinx.serialization.Serializable

/**
 * The single persisted save file. Defaults intentionally satisfy "only
 * Noah's Ark is unlocked" even for a brand-new or corrupted profile (see
 * spec section 20 — sensible defaults, no crashes on missing/corrupt data).
 */
@Serializable
data class PlayerProfile(
    val id: String = "local_player",
    val character: CharacterCustomization = CharacterCustomization(),
    val unlockedChapters: Set<ChapterId> = setOf(ChapterId.NOAHS_ARK),
    val completedChapters: Set<ChapterId> = emptySet(),
    val progressByChapter: Map<ChapterId, AdventureProgress> = emptyMap(),
    val stars: Int = 0,
    val badges: Set<String> = emptySet(),
    val scriptureCards: Set<String> = emptySet(),
    val audioSettings: AudioSettings = AudioSettings(),
) {
    companion object {
        val DEFAULT = PlayerProfile()
    }
}
