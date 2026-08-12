package com.bibleadventures.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.bibleadventures.R
import com.bibleadventures.domain.model.AudioSettings
import com.bibleadventures.domain.repository.PlayerProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.Locale

/**
 * Below 1.0 = deeper/slower than the platform TTS default — see the "Calm
 * Storyteller" tuning note where these are applied. Pitch dropped further
 * (0.80 -> 0.65) per direct follow-up feedback that the first pass still
 * read as high-pitched — most devices' default TTS voice is female, and a
 * mild pitch cut on a female voice doesn't read as "deep male," it just
 * reads as a slightly lower female voice. 0.65 is close to the floor where
 * some engines start introducing audible distortion, but paired with
 * [selectDeepMaleVoice] actually swapping to a male voice where the device
 * offers one, so the pitch cut needed to *feel* deep is smaller than if
 * pitch were doing all the work alone.
 */
private const val NARRATION_PITCH = 0.65f
private const val NARRATION_SPEECH_RATE = 0.82f

/**
 * Requested explicitly rather than trusting the device's configured default
 * TTS engine — some vendors ship their own engine with few or no voices, or
 * none with gender-labeled names, which would make [RealAudioController.selectDeepMaleVoice]
 * silently find nothing to select. Falls back to the device default engine
 * if Google's isn't installed (see `initializeTts`), so this is a
 * preference, not a hard requirement.
 */
private const val GOOGLE_TTS_ENGINE_PACKAGE = "com.google.android.tts"

/**
 * Real playback: [SoundPool] for short SFX, one looping [MediaPlayer] for
 * background music, [TextToSpeech] for narration — all placeholder,
 * synthesized audio (see `scripts/generate_placeholder_audio.py`), swappable
 * for licensed assets later without touching this file's logic, same as
 * every placeholder drawable in this app. Never crashes on missing audio
 * hardware/engines — degrades to silence, same defensive posture as
 * [com.bibleadventures.domain.model.PlayerProfile]'s corrupted-save fallback.
 */
class RealAudioController(
    context: Context,
    playerProfileRepository: PlayerProfileRepository,
) : AudioController {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var settings: AudioSettings = AudioSettings()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundResIds: Map<SoundEffect, Int> = mapOf(
        SoundEffect.MATCH_SUCCESS to R.raw.match_success,
        SoundEffect.SCENE_TRANSITION to R.raw.scene_transition,
        SoundEffect.REWARD_CELEBRATION to R.raw.reward_celebration,
        SoundEffect.ITEM_COLLECTED to R.raw.item_collected,
        SoundEffect.TARGET_HIT to R.raw.target_hit,
        SoundEffect.OBSTACLE_DODGED to R.raw.obstacle_dodged,
        SoundEffect.TRUMPET_FANFARE to R.raw.trumpet_fanfare,
    )

    private val soundIds: Map<SoundEffect, Int> = soundResIds.mapValues { (_, resId) -> soundPool.load(appContext, resId, 1) }
    private val loadedSoundIds = mutableSetOf<Int>()

    private val musicResIds: Map<MusicTrack, Int> = mapOf(
        MusicTrack.WORLD_MAP to R.raw.world_map_music,
        MusicTrack.ADVENTURE to R.raw.adventure_music,
    )
    private var musicPlayer: MediaPlayer? = null
    private var currentMusicTrack: MusicTrack? = null

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedSoundIds += sampleId
        }
        playerProfileRepository.profile
            .map { it.audioSettings }
            .distinctUntilChanged()
            .onEach { newSettings -> applySettingsChange(newSettings) }
            .launchIn(scope)
        initializeTts(preferGoogleEngine = true)
    }

    /**
     * [preferGoogleEngine] tries Google's TTS engine by package name first
     * (see [GOOGLE_TTS_ENGINE_PACKAGE]); if that engine isn't installed on
     * this device, `TextToSpeech`'s init callback reports a non-SUCCESS
     * status and this retries once more with the device's own default
     * engine — same "never crash on missing audio hardware/engines,
     * degrade gracefully" posture as the rest of this class, just applied
     * to engine selection instead of playback itself.
     */
    private fun initializeTts(preferGoogleEngine: Boolean) {
        val onInit = TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.SUCCESS) {
                if (preferGoogleEngine) initializeTts(preferGoogleEngine = false)
                return@OnInitListener
            }
            ttsReady = true
            val engine = tts ?: return@OnInitListener
            engine.language = Locale.US
            // Selecting an actual male-labeled voice first (best-effort —
            // most devices' default TTS voice is female, and pitch alone
            // can't fully turn a female voice into a convincing male one),
            // since setVoice(...) can itself reset pitch/rate back to that
            // voice's own defaults on some engines — setPitch/setSpeechRate
            // must run *after* it to actually stick.
            selectDeepMaleVoice(engine)
            engine.setPitch(NARRATION_PITCH)
            engine.setSpeechRate(NARRATION_SPEECH_RATE)
        }
        tts = if (preferGoogleEngine) {
            TextToSpeech(appContext, onInit, GOOGLE_TTS_ENGINE_PACKAGE)
        } else {
            TextToSpeech(appContext, onInit)
        }
    }

    /**
     * Best-effort — not every device's TTS engine exposes multiple voices,
     * or labels any of them by gender at all, so this silently leaves the
     * engine's own default voice in place if nothing matches (never
     * crashes, same defensive posture as the rest of this class). Prefers,
     * in order: a male-named voice in English that doesn't need a network
     * connection (so narration keeps working offline), then any
     * male-named English voice, then any male-named voice at all.
     */
    private fun selectDeepMaleVoice(engine: TextToSpeech) {
        val voices = runCatching { engine.voices }.getOrNull() ?: return

        fun isMaleNamed(voice: Voice) = voice.name.contains("male", ignoreCase = true) && !voice.name.contains("female", ignoreCase = true)

        val bestMatch = voices.firstOrNull { it.locale == Locale.US && !it.isNetworkConnectionRequired && isMaleNamed(it) }
            ?: voices.firstOrNull { it.locale == Locale.US && isMaleNamed(it) }
            ?: voices.firstOrNull(::isMaleNamed)

        if (bestMatch != null) {
            runCatching { engine.voice = bestMatch }
        }
    }

    // Reacts to a toggle flip immediately instead of waiting for the next
    // playMusic()/speak() call — without this, muting music or narration
    // mid-playback had no effect until the current screen was re-entered.
    private fun applySettingsChange(newSettings: AudioSettings) {
        val previous = settings
        settings = newSettings
        if (previous.musicEnabled && !newSettings.musicEnabled) {
            runCatching { musicPlayer?.pause() }
        } else if (!previous.musicEnabled && newSettings.musicEnabled) {
            currentMusicTrack?.let { startMusicPlayback(it) }
        }
        if (!newSettings.narrationEnabled) {
            tts?.stop()
        }
    }

    // Deliberately not MediaPlayer.create(...), which calls the blocking prepare()
    // internally — this would stall the calling thread (typically the Compose
    // main thread, from a screen-entry LaunchedEffect). setDataSource + prepareAsync()
    // keeps playMusic non-blocking; playback starts once onPreparedListener fires.
    private fun startMusicPlayback(track: MusicTrack) {
        val resId = musicResIds[track] ?: return
        musicPlayer?.release()
        musicPlayer = MediaPlayer().apply {
            appContext.resources.openRawResourceFd(resId).use { afd ->
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            isLooping = true
            // Guards against the setting having been toggled off while
            // prepareAsync() was still in flight.
            setOnPreparedListener { player -> if (settings.musicEnabled) player.start() }
            prepareAsync()
        }
    }

    override fun playMusic(track: MusicTrack) {
        currentMusicTrack = track
        if (!settings.musicEnabled) return
        startMusicPlayback(track)
    }

    override fun stopMusic() {
        currentMusicTrack = null
        runCatching { musicPlayer?.pause() }
    }

    override fun playSfx(effect: SoundEffect) {
        if (!settings.soundEffectsEnabled) return
        val soundId = soundIds[effect] ?: return
        if (soundId in loadedSoundIds) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    override fun speak(text: String) {
        if (!settings.narrationEnabled || !ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    override fun stopSpeaking() {
        tts?.stop()
    }
}
