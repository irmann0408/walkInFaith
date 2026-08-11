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
import kotlinx.coroutines.flow.launchIn
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

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedSoundIds += sampleId
        }
        playerProfileRepository.profile
            .onEach { settings = it.audioSettings }
            .launchIn(scope)
        tts = TextToSpeech(appContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts?.language = Locale.getDefault()
        }
    }

    // Deliberately not MediaPlayer.create(...), which calls the blocking prepare()
    // internally — this would stall the calling thread (typically the Compose
    // main thread, from a screen-entry LaunchedEffect). setDataSource + prepareAsync()
    // keeps playMusic non-blocking; playback starts once onPreparedListener fires.
    override fun playMusic(track: MusicTrack) {
        if (!settings.musicEnabled) return
        val resId = musicResIds[track] ?: return
        musicPlayer?.release()
        musicPlayer = MediaPlayer().apply {
            appContext.resources.openRawResourceFd(resId).use { afd ->
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            isLooping = true
            setOnPreparedListener { it.start() }
            prepareAsync()
        }
    }

    override fun stopMusic() {
        musicPlayer?.pause()
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
