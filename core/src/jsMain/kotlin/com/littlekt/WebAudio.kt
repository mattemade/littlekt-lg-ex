package com.littlekt

import com.littlekt.audio.globalAudioContext

class WebAudio: Audio {
    override fun setListenerPosition(x: Float, y: Float, z: Float) {
        globalAudioContext?.listener?.apply {
            positionX.value = x
            positionY.value = y
            positionZ.value = z
        }
    }
}
