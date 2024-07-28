package com.littlekt.audio

import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.events.Event

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
internal external class JsSound {
    var duration: Double = definedExternally
}

internal external interface JsSoundDestination

internal external class JsAudioParam<T> {
    var value: T
}

internal open external class JsAudioNode {
    fun connect(destination: JsSoundDestination)
    fun disconnect()
}

internal external class JsAudioGainNode : JsAudioNode, JsSoundDestination {
    val gain: JsAudioParam<Float>
}

internal enum class DistanceModelType {
    linear,
    inverse,
    exponential
}

internal enum class PanningModelType {
    equalpower,
    HRTF
}

internal external class JsAudioPannerNode : JsAudioNode, JsSoundDestination {
    val positionX: JsAudioParam<Float>
    val positionY: JsAudioParam<Float>
    var refDistance: Float = definedExternally
    var maxDistance: Float = definedExternally
    var rolloffFactor: Float = definedExternally
    var distanceModelType: DistanceModelType = definedExternally
    var panningModelType: PanningModelType = definedExternally
}

internal external class JsAudioBufferSourceNode : JsAudioNode {
    var buffer: JsSound

    var loop: Boolean

    val playbackRate: JsAudioParam<Float>

    var onended: () -> Unit

    fun start(
        delay: Float = definedExternally,
        offset: Float = definedExternally,
        duration: Float = definedExternally
    )

    fun stop(time: Double)
}

internal external class JsAudioListener {
    val positionX: JsAudioParam<Float>
    val positionY: JsAudioParam<Float>
    val positionZ: JsAudioParam<Float>
}

internal external class AudioContext {

    val state: String

    val currentTime: Double

    fun resume()

    val destination: JsSoundDestination

    var listener: JsAudioListener

    fun decodeAudioData(
        bytes: ArrayBuffer,
        onLoad: (buffer: JsSound) -> Unit,
        onError: (event: Event) -> Unit
    )

    fun createBufferSource(): JsAudioBufferSourceNode
    fun createGain(): JsAudioGainNode
    fun createPanner(): JsAudioPannerNode
}

// Certain web browsers only allow playing sounds if it happens in response to a limited list of user actions.
// Once the AudioContext is correctly resumed, the app could play any sound anytime without restrictions.
// This method ensures that AudioContext is resumed as early as possible.
internal val globalAudioContext: AudioContext? = (js(
    """
            var AudioContextClass = window.AudioContext || window.webkitAudioContext;
            if (AudioContextClass) {
                var audioContext = new AudioContextClass();
                if (audioContext !== 'running') {
                    var userInteractionEventNames = [ 'mousedown', 'pointerdown', 'touchstart', 'keydown' ];
    
                    function resumeAudioContext(ignoredEventName) {
                        audioContext.resume();
                        userInteractionEventNames.forEach(function(eventName) {
                            document.removeEventListener(eventName, resumeAudioContext);
                        });
                    }
                    
                    userInteractionEventNames.forEach(function (eventName) {
                        document.addEventListener(eventName, resumeAudioContext);
                    });
                }
                return audioContext;
            }
            return null;
            """
) as? AudioContext)
