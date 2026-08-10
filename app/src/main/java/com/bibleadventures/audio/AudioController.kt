package com.bibleadventures.audio

enum class MusicTrack { WORLD_MAP, NOAHS_ARK }

enum class SoundEffect { MATCH_SUCCESS, SCENE_TRANSITION, REWARD_CELEBRATION, ITEM_COLLECTED }

/**
 * Audio abstraction (spec section 14). Milestone 4 only wires silent call
 * sites via [NoOpAudioController] — real playback and the Settings on/off
 * toggles are Milestone 7 (Polish) scope.
 */
interface AudioController {
    fun playMusic(track: MusicTrack)
    fun stopMusic()
    fun playSfx(effect: SoundEffect)
}

class NoOpAudioController : AudioController {
    override fun playMusic(track: MusicTrack) = Unit
    override fun stopMusic() = Unit
    override fun playSfx(effect: SoundEffect) = Unit
}
