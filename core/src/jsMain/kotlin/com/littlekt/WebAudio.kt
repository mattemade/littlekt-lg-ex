package com.littlekt

import com.littlekt.audio.globalAudioContext
import com.littlekt.audio.setPositionCompat

class WebAudio: Audio {

    override fun isReady(): Boolean =
        globalAudioContext != null

    override fun setListenerPosition(x: Float, y: Float, z: Float) {
        globalAudioContext?.listener?.apply {
            setPositionCompat(x, y, z)
        }
    }

    override fun suspend() {
        globalAudioContext?.suspend()
    }

    override fun resume() {
        globalAudioContext?.resume()
    }
}
