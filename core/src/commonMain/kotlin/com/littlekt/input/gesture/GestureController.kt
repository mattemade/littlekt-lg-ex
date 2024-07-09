package com.littlekt.input.gesture

import com.littlekt.async.KtScope
import com.littlekt.input.Input
import com.littlekt.input.InputProcessor
import com.littlekt.input.Pointer
import com.littlekt.math.MutableVec2f
import com.littlekt.util.internal.epochMillis
import com.littlekt.util.milliseconds
import com.littlekt.util.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * An [InputProcessor] that handles input events and emits gesture related events such as flinging, panning, zooming,
 * and pinching and sends them to a [GestureProcessor]. Based off of libGDX GestureDetector.
 *
 * @author Colton Daily
 * @date 10/24/2022
 */
class GestureController(
    private val input: Input,
    var tapWidth: Float = 20f,
    var tapHeight: Float = tapWidth,
    var tapCountInterval: Duration = 400.milliseconds,
    var longPressDuration: Duration = 1100.milliseconds,
    var maxFlingDuration: Duration = Duration.INFINITE,
    var processor: GestureProcessor,
) : InputProcessor {

    constructor(
        input: Input,
        tapWidth: Float = 20f,
        tapHeight: Float = tapWidth,
        tapCountInterval: Duration = 400.milliseconds,
        longPressDuration: Duration = 1100.milliseconds,
        maxFlingDuration: Duration = Duration.INFINITE,
        setup: GestureProcessorBuilder.() -> Unit,
    ) : this(
        input,
        tapWidth,
        tapHeight,
        tapCountInterval,
        longPressDuration,
        maxFlingDuration,
        GestureProcessorBuilder().apply {
            setup()
        }.build()
    )

    private val pointer1 = MutableVec2f()
    private val pointer2 = MutableVec2f()
    private val initialPos1 = MutableVec2f()
    private val initialPos2 = MutableVec2f()

    private val tracker = VelocityTracker()
    private var tapCenterX = 0f
    private var tapCenterY = 0f
    private var touchDownTime = 0L

    private var tapCount = 0
    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f
    private var lastTapPointer: Pointer? = null
    private var inTapRectangle = false
    private var panning = false
    private var pinching = false
    private var longPressFired = false

    private val pointers = mutableSetOf<Pointer>()

    private var longPressJob: Job? = null

    override fun touchDown(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        if (pointers.size > 1) return false
        pointers += pointer
        val idx = pointers.indexOf(pointer)
        if (idx == 0) {
            pointer1.set(screenX, screenY)
            touchDownTime = input.currentEventTime
            tracker.start(screenX, screenY, touchDownTime)

            if (input.isTouching(2)) {
                inTapRectangle = false
                pinching = true
                initialPos1.set(pointer1)
                initialPos2.set(pointer2)
                longPressJob?.cancel()
            } else {
                inTapRectangle = true
                pinching = false
                longPressFired = false
                tapCenterX = screenX
                tapCenterY = screenY
                if (longPressJob?.isActive != true) {
                    longPressJob = KtScope.launch {
                        delay(longPressDuration)
                        processor.longPress(screenX, screenY)
                    }
                }
            }
        } else {
            pointer2.set(screenX, screenY)
            inTapRectangle = false
            pinching = true
            initialPos1.set(pointer1)
            initialPos2.set(pointer2)
            longPressJob?.cancel()
        }

        return processor.touchDown(screenX, screenY, pointer)
    }

    override fun touchDragged(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        if (!pointers.contains(pointer)) return false
        if (longPressFired) return false
        val idx = pointers.indexOf(pointer)

        if (idx == 0) {
            pointer1.set(screenX, screenY)
        } else {
            pointer2.set(screenX, screenY)
        }

        if (pinching) {
            val result = processor.pinch(initialPos1, initialPos2, pointer1, pointer2)
            return processor.zoom(initialPos1.distance(initialPos2), pointer1.distance(pointer2)) || result
        }

        tracker.update(screenX, screenY, input.currentEventTime)

        if (inTapRectangle && !isWithinTapRectangle(screenX, screenY, tapCenterX, tapCenterY)) {
            longPressJob?.cancel()
            inTapRectangle = false
        }

        if (!inTapRectangle) {
            panning = true
            return processor.pan(screenX, screenY, tracker.dx, tracker.dy)
        }

        return false
    }

    override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        if (!pointers.contains(pointer)) return false
        val idx = pointers.indexOf(pointer)
        pointers -= pointer

        if (inTapRectangle && !isWithinTapRectangle(screenX, screenY, tapCenterX, tapCenterY)) {
            inTapRectangle = false
        }

        val wasPanning = panning
        panning = false

        longPressJob?.cancel()
        if (longPressFired) return false

        if (inTapRectangle) {
            if (lastTapPointer != pointer || epochMillis() - lastTapTime > tapCountInterval.milliseconds || !isWithinTapRectangle(
                    screenX,
                    screenY,
                    lastTapX,
                    lastTapY
                )
            ) {
                tapCount = 0
            }
            tapCount++
            lastTapTime = epochMillis()
            lastTapX = screenX
            lastTapY = screenY
            lastTapPointer = pointer
            touchDownTime = 0
            return processor.tap(screenX, screenY, tapCount, pointer)
        }

        if (pinching) {
            pinching = false
            processor.pinchStop()
            panning = true
            if (idx == 0) {
                tracker.start(pointer2.x, pointer2.y, input.currentEventTime)
            } else {
                tracker.start(pointer1.x, pointer1.y, input.currentEventTime)
            }
            return false
        }

        var handled = false
        if (wasPanning && !panning) {
            handled = processor.panStop(screenX, screenY, pointer)
        }

        val time = input.currentEventTime
        if (time - touchDownTime <= maxFlingDuration.seconds) {
            tracker.update(screenX, screenY, time)
            handled = processor.fling(tracker.velocityX * 1000f, tracker.velocityY * 1000f, pointer) || handled
        }

        touchDownTime = 0
        return handled
    }

    fun cancel() {
        longPressJob?.cancel()
        longPressFired = true
    }

    private fun isWithinTapRectangle(x: Float, y: Float, cx: Float, cy: Float): Boolean {
        return abs(x - cx) < tapWidth && abs(y - cy) < tapHeight
    }

    private class VelocityTracker {
        var sampleSize = 10
        var lastX = 0f
        var lastY = 0f
        var dx = 0f
        var dy = 0f
        var lastTime = 0L
        var numSamples = 0
        val meanX = FloatArray(sampleSize)
        val meanY = FloatArray(sampleSize)
        val meanTime = LongArray(sampleSize)

        val velocityX: Float
            get() {
                val meanX = getAverage(meanX, numSamples)
                val meanTime = getAverage(meanTime, numSamples).toFloat()
                if (meanTime == 0f) return 0f
                return meanX / meanTime
            }

        val velocityY: Float
            get() {
                val meanY = getAverage(meanY, numSamples)
                val meanTime = getAverage(meanTime, numSamples).toFloat()
                if (meanTime == 0f) return 0f
                return meanY / meanTime
            }

        fun start(x: Float, y: Float, timeStamp: Long) {
            lastX = x
            lastY = y
            dx = 0f
            dy = 0f
            numSamples = 0
            repeat(sampleSize) {
                meanX[it] = 0f
                meanY[it] = 0f
                meanTime[it] = 0L
            }
            lastTime = timeStamp
        }

        fun update(x: Float, y: Float, currentTime: Long) {
            dx = x - lastX
            dy = y - lastY
            lastX = x
            lastY = y
            val dt = currentTime - lastTime
            lastTime = currentTime
            val idx = numSamples % sampleSize
            meanX[idx] = dx
            meanY[idx] = dy
            meanTime[idx] = dt
            numSamples++
        }

        private fun getAverage(values: FloatArray, numSamples: Int): Float {
            val samples = min(sampleSize, numSamples)
            var sum = 0f
            repeat(samples) {
                sum += values[it]
            }
            return sum / numSamples
        }

        private fun getAverage(values: LongArray, numSamples: Int): Long {
            val samples = min(sampleSize, numSamples)
            var sum = 0L
            repeat(samples) {
                sum += values[it]
            }
            return sum / numSamples
        }
    }
}

/**
 * Creates a new [GestureController] using a [GestureProcessorBuilder] and calls [Input.addInputProcessor] when built.
 * @return the built [GestureController]
 */
fun Input.gestureController(setup: GestureProcessorBuilder.() -> Unit): GestureController {
    return GestureController(this, setup = setup).also { addInputProcessor(it) }
}

