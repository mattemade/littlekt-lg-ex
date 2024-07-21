package com.littlekt.audio

import com.littlekt.Releasable

internal class WebAudioExPipeline(
    jsSound: JsSound,
    audioContext: AudioContext
) : Releasable {
    val gain: JsAudioGainNode = audioContext.createGain().apply {
        connect(audioContext.destination)
    }
    val source: JsAudioBufferSourceNode = audioContext.createBufferSource().apply {
        buffer = jsSound
        connect(gain)
    }

    override fun release() {
        source.disconnect()
        gain.disconnect()
    }
}