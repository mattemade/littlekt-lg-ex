package com.littlekt.audio

internal class WebAudioStreamEx(private val audio: WebAudioEx) : AudioStreamEx {
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

    private var isPaused: Boolean = false
    override val paused: Boolean
        get() = isPaused

    override suspend fun play(
        volume: Float,
        positionX: Float,
        positionY: Float,
        referenceDistance: Float,
        maxDistance: Float,
        rolloffFactor: Float,
        loop: Boolean
    ) {
        audio.play(
            volume,
            positionX,
            positionY,
            referenceDistance,
            maxDistance,
            rolloffFactor,
            loop
        )
        isPaused = false
    }

    override suspend fun play(volume: Float, loop: Boolean) {
        audio.play(volume, 0f, 0f, 10000f, 10000f, 0f, loop)
        isPaused = false
    }

    override fun setPosition(positionX: Float, positionY: Float) {
        audio.pipeline.panner.positionX.value = positionX
        audio.pipeline.panner.positionY.value = positionY
    }

    override fun stop() {
        audio.stop()
        isPaused = false
    }

    override fun resume() {
        audio.resume()
        isPaused = false
    }

    override fun pause() {
        audio.pause()
        isPaused = true
    }

    override fun release() {
        audio.release()
    }
}
