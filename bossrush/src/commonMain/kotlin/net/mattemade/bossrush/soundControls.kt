package net.mattemade.bossrush

import com.littlekt.audio.AudioClipEx
import com.littlekt.audio.AudioStreamEx
import com.littlekt.file.Vfs
import com.littlekt.math.Vec2f
import com.littlekt.math.Vec3f
import com.littlekt.util.seconds
import kotlin.time.Duration

private val soundsPlayedAt = mutableMapOf<AudioClipEx, Float>()
private val packsPlayedAt = mutableMapOf<SoundPack, Float>()
private var time: Float = 0f

private var currentlyPlayingAmbient = mutableListOf<AudioStreamEx>()
private var currentlyPlayingMusic: AudioStreamEx? = null

fun updateSoundTime(dt: Duration) {
    time += dt.seconds
    currentlyPlayingMusic?.let {
        it.volume = SOUND_VOLUME / 160f
    }
    currentlyPlayingAmbient.forEach {
        it.volume =  SOUND_VOLUME / 160f
    }
}


fun SoundPack.maybePlay(position: Vec2f) {
    val playedLastTimeAt = packsPlayedAt.getOrElse(this) { -10000f }
    val volumeMultiplier = minOf(1f, (time - playedLastTimeAt) * 3f)
    sound.play(volume = SOUND_VOLUME * volumeMultiplier, positionX = position.x, positionY = position.y)
    packsPlayedAt[this] = time
}

fun AudioClipEx.maybePlay(position: Vec2f) {
    val playedLastTimeAt = soundsPlayedAt.getOrElse(this) { -10000f }
    val volumeMultiplier = minOf(1f, (time - playedLastTimeAt) * 3f)
    play(volume = SOUND_VOLUME * volumeMultiplier, positionX = position.x, positionY = position.y)
    soundsPlayedAt[this] = time
}

fun AudioStreamEx.maybePlayMusic(vfs: Vfs) {
    if (currentlyPlayingMusic == this) {
        if (currentlyPlayingMusic?.paused == true) {
            resume()
        }
    } else {
        currentlyPlayingMusic?.stop()
        currentlyPlayingMusic = this
        vfs.launch {
            play(volume = SOUND_VOLUME / 160f, loop = true)
        }
    }
}

fun pauseMusic() {
    currentlyPlayingMusic?.pause()
}

fun fullStopMusic() {
    currentlyPlayingMusic?.stop() // does not work though :<
    currentlyPlayingMusic = null
}

suspend fun AudioStreamEx.playAmbient() {
    currentlyPlayingAmbient += this
    play(volume = SOUND_VOLUME / 160f, loop = true)
}

fun cameraMoved(position: Vec3f) {
    currentlyPlayingMusic?.setPosition(position.x, position.y)
    currentlyPlayingAmbient.forEach {
        it.setPosition(position.x, position.y)
    }
}