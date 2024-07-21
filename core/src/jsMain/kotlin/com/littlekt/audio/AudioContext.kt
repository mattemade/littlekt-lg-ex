package com.littlekt.audio

import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.events.Event

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
external class JsSound {
    var duration: Double = definedExternally
}

external interface JsSoundDestination

external class JsAudioParam<T> {
    var value: T
}

open external class JsAudioNode {
    fun connect(destination: JsSoundDestination)
    fun disconnect()
}

external class JsAudioGainNode: JsAudioNode, JsSoundDestination {
    val gain: JsAudioParam<Float>
}

external class JsAudioBufferSourceNode: JsAudioNode {
    var buffer: JsSound

    var loop: Boolean

    val playbackRate: JsAudioParam<Float>

    var onended: () -> Unit

    fun start(delay: Float = definedExternally, offset: Float = definedExternally, duration: Float = definedExternally)
    fun stop(time: Double)
}

open external class AudioContext {

    val state: String

    val currentTime: Double

    fun resume()

    val destination: JsSoundDestination

    fun decodeAudioData(bytes: ArrayBuffer, onLoad: (buffer: JsSound) -> Unit, onError: (event: Event) -> Unit)

    fun createBufferSource(): JsAudioBufferSourceNode
    fun createGain(): JsAudioGainNode
}

// Certain web browsers only allow playing sounds if it happens in response to a limited list of user actions.
// Once the AudioContext is correctly resumed, the app could play any sound anytime without restrictions.
// This method ensures that AudioContext is resumed as early as possible.
val globalAudioContext: AudioContext? = (js(
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
