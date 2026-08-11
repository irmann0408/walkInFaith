package com.bibleadventures.audio

/** ADVENTURE is one shared gameplay-music loop reused by every chapter, not a track per chapter. */
enum class MusicTrack { WORLD_MAP, ADVENTURE }

enum class SoundEffect { MATCH_SUCCESS, SCENE_TRANSITION, REWARD_CELEBRATION, ITEM_COLLECTED, TARGET_HIT, OBSTACLE_DODGED, TRUMPET_FANFARE }

/**
 * Audio abstraction (spec section 14), including narration (spec section
 * 17's narration toggle). [RealAudioController] is the real implementation
 * (SoundPool/MediaPlayer/TextToSpeech); [NoOpAudioController] remains
 * available for tests/contexts that want silence.
 */
interface AudioController {
    fun playMusic(track: MusicTrack)
    fun stopMusic()
    fun playSfx(effect: SoundEffect)
    fun speak(text: String)
    fun stopSpeaking()
}

class NoOpAudioController : AudioController {
    override fun playMusic(track: MusicTrack) = Unit
    override fun stopMusic() = Unit
    override fun playSfx(effect: SoundEffect) = Unit
    override fun speak(text: String) = Unit
    override fun stopSpeaking() = Unit
}
