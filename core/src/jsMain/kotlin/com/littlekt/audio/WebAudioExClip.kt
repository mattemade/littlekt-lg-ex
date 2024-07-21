package com.littlekt.audio

import kotlin.time.Duration

internal class WebAudioExClip(private val audio: WebAudioEx) : AudioClip {
    override var volume: Float
        get() = audio.volume
        set(value) {
            audio.volume = value
        }
    override val duration: Duration
        get() = audio.duration

    override fun play(volume: Float, loop: Boolean) {
        audio.preparePipeline()
        audio.play(volume, loop)
        if (!loop) {
            audio.setOnEnded(WebAudioExPipeline::release)
        }
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
