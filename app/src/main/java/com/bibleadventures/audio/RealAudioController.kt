package com.bibleadventures.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.speech.tts.TextToSpeech
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
        tts = TextToSpeech(appContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts?.language = Locale.getDefault()
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
