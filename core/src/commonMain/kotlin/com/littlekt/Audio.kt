package com.littlekt

import com.littlekt.audio.AudioClipEx
import com.littlekt.audio.AudioStreamEx

interface Audio {
    /**
     * Set the listener position for spatial sound. This position is used to control the pan and
     * volume of all the spatial sounds ([AudioClipEx], [AudioStreamEx]).
     */
    fun setListenerPosition(x: Float, y: Float, z: Float)
}
