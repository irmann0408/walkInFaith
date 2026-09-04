package com.bibleadventures.audio

/** ADVENTURE is one shared gameplay-music loop reused by every chapter, not a track per chapter. */
enum class MusicTrack { WORLD_MAP, ADVENTURE }

enum class SoundEffect { MATCH_SUCCESS, SCENE_TRANSITION, REWARD_CELEBRATION, ITEM_COLLECTED, TARGET_HIT, OBSTACLE_DODGED, TRUMPET_FANFARE }

/**
 * A real recorded line for the player's own character (not the generic
 * narrator TTS voice) — recorded twice per line, once for each
 * [com.bibleadventures.domain.model.Appearance], so it matches whichever
 * character the player customized. Currently only Noah's Ark and David &
 * Goliath have recordings; every other chapter still uses [AudioController.speak].
 */
enum class CharacterVoiceLine {
    NOAH_INTRO,
    NOAH_FIND_TOOLS_INTRO,
    NOAH_NOT_A_TOOL,
    NOAH_BUILDING_ARK,
    NOAH_MATCHING_INTRO,
    NOAH_ANIMALS_ENTERING,
    NOAH_ORGANIZE_INTRO,
    NOAH_GREAT_FLOOD,
    NOAH_DOVE_AND_LAND,
    NOAH_RAINBOW_PROMISE,
    NOAH_LESSON,
    DAVID_FAITHFUL_SHEPHERD,
    DAVID_SHEEP_COUNTING_INTRO,
    DAVID_GIANTS_CHALLENGE,
    DAVID_ARRIVES,
    DAVID_HEAVY_ARMOR,
    DAVID_CHOOSE_STONES_INTRO,
    DAVID_FIVE_SMOOTH_STONES,
    DAVID_SLING_PRACTICE_INTRO,
    DAVID_SLING_ESCAPED,
    DAVID_VICTORY,
    DAVID_LESSON,
    FEEDBACK_GREAT_JOB,
    FEEDBACK_TRY_ANOTHER_ONE,
}

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

    /**
     * [onCompletion] fires once playback genuinely finishes, or immediately
     * if narration is disabled/the line has no recording — a caller that
     * needs to resume something it paused for this line (see
     * `StoryVideoScreen`'s tap-to-interrupt-narration flow) can always rely
     * on it firing exactly once.
     */
    fun playCharacterLine(line: CharacterVoiceLine, onCompletion: () -> Unit = {})
}

class NoOpAudioController : AudioController {
    override fun playMusic(track: MusicTrack) = Unit
    override fun stopMusic() = Unit
    override fun playSfx(effect: SoundEffect) = Unit
    override fun speak(text: String) = Unit
    override fun stopSpeaking() = Unit
    override fun playCharacterLine(line: CharacterVoiceLine, onCompletion: () -> Unit) = onCompletion()
}
