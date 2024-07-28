package com.littlekt.audio

import com.littlekt.Releasable

internal class WebAudioPipeline(
    jsSound: JsSound,
    audioContext: AudioContext
) : Releasable {
    val panner: JsAudioPannerNode = audioContext.createPanner().apply {
        distanceModelType = DistanceModelType.linear
        panningModelType = PanningModelType.HRTF
        connect(audioContext.destination)
    }
    val gain: JsAudioGainNode = audioContext.createGain().apply {
        connect(panner)
    }
    val source: JsAudioBufferSourceNode = audioContext.createBufferSource().apply {
        buffer = jsSound
        connect(gain)
    }

    override fun release() {
        source.disconnect()
        gain.disconnect()
        panner.disconnect()
    }
}
