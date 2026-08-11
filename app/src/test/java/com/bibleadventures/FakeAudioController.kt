package com.bibleadventures

import com.bibleadventures.audio.AudioController
import com.bibleadventures.audio.MusicTrack
import com.bibleadventures.audio.SoundEffect

/** Records every played SFX so ViewModel unit tests can assert "a sound played on X, not on Y." */
class FakeAudioController : AudioController {
    val playedEffects = mutableListOf<SoundEffect>()
    val spokenText = mutableListOf<String>()

    override fun playMusic(track: MusicTrack) = Unit
    override fun stopMusic() = Unit
    override fun playSfx(effect: SoundEffect) {
        playedEffects += effect
    }
    override fun speak(text: String) {
        spokenText += text
    }
    override fun stopSpeaking() = Unit
}
