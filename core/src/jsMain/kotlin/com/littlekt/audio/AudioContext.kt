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
    val positionX: JsAudioParam<Float>?
    val positionY: JsAudioParam<Float>?

    // deprecated, but usable in firefox
    fun setPosition(x: Float, y: Float)

    var refDistance: Float = definedExternally
    var maxDistance: Float = definedExternally
    var rolloffFactor: Float = definedExternally
    var distanceModelType: DistanceModelType = definedExternally
    var panningModelType: PanningModelType = definedExternally
}

internal fun JsAudioPannerNode.setPositionCompat(x: Float, y: Float) {
    if (positionX == null || positionY == null) {
        // firefox hack
        setPosition(x, y)
        return
    }
    this.positionX.value = x
    this.positionY.value = y
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
    val positionX: JsAudioParam<Float>?
    val positionY: JsAudioParam<Float>?
    val positionZ: JsAudioParam<Float>?

    // deprecated, but usable in firefox
    fun setPosition(x: Float, y: Float, z: Float)
}

internal fun JsAudioListener.setPositionCompat(x: Float, y: Float, z: Float) {
    if (positionX == null || positionY == null || positionZ == null) {
        // firefox hack
        setPosition(x, y, z)
        return
    }
    this.positionX.value = x
    this.positionY.value = y
    this.positionZ.value = z
}

internal external class AudioContext {

    val state: String

    val currentTime: Double

    fun resume()

    fun suspend()

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
// TODO mobile Firefox doesn't allow creating an AudioContext before any tap is done
// how to fix that? maybe don't init the context until any of these events happen?
private var globalAudioContextCache: AudioContext? = null
internal val globalAudioContext: AudioContext?
    get() = globalAudioContextCache ?: (js(
        """
            var AudioContextClass = window.AudioContext || window.webkitAudioContext;
            if (AudioContextClass) {
                return new AudioContextClass();
            }
            return null;
            """
    ) as? AudioContext)?.also { globalAudioContextCache = it }
