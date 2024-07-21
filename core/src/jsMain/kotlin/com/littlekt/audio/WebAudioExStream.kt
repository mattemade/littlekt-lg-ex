package com.littlekt.audio

internal class WebAudioExStream(private val audio: WebAudioEx) : AudioStream {
    override var volume: Float
        get() = audio.volume
        set(value) {
            audio.volume = value
        }
    override var looping: Boolean
        get() = audio.looping
        set(value) {
            audio.looping = value
        }
    override val playing: Boolean
        get() = audio.playing

    override suspend fun play(volume: Float, loop: Boolean) {
        audio.play(volume, loop)
    }

    override fun stop() {
        audio.stop()
    }

    override fun resume() {
        audio.resume()
    }

    override fun pause() {
        audio.pause()
    }

    override fun release() {
        audio.release()
    }
}
