package com.littlekt

import com.littlekt.audio.globalAudioContext

class WebAudio: Audio {

    override fun isReady(): Boolean =
        globalAudioContext != null

    override fun setListenerPosition(x: Float, y: Float, z: Float) {
        globalAudioContext?.listener?.apply {
            if (positionX == null || positionY == null || positionZ == null) {
                // firefox hack
                setPosition(x, y, z)
                return@apply
            }
            positionX.value = x
            positionY.value = y
            positionZ.value = z
        }
    }

    override fun suspend() {
        globalAudioContext?.suspend()
    }

    override fun resume() {
        globalAudioContext?.resume()
    }
}
