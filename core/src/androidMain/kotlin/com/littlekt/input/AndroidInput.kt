package com.littlekt.input

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnKeyListener
import android.view.View.OnTouchListener
import android.view.inputmethod.InputMethodManager
import com.littlekt.AndroidGraphics
import com.littlekt.async.KtScope
import com.littlekt.math.geom.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 2/12/2022
 */
class AndroidInput(private val androidCtx: Context, private val graphics: AndroidGraphics) : Input, OnTouchListener,
    OnKeyListener {

    private val vibrator = androidCtx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private val inputCache = InputCache()

    private val _inputProcessors = mutableListOf<InputProcessor>()
    override val inputProcessors: List<InputProcessor>
        get() = _inputProcessors

    override val gamepads: Array<GamepadInfo> = Array(8) { GamepadInfo(it) }

    private val _connectedGamepads = mutableListOf<GamepadInfo>()
    override val connectedGamepads: List<GamepadInfo>
        get() = _connectedGamepads
    override val catchKeys: MutableList<Key> = mutableListOf()

    private val touchX = IntArray(MAX_TOUCHES)
    private val touchY = IntArray(MAX_TOUCHES)
    private val touchDeltaX = IntArray(MAX_TOUCHES)
    private val touchDeltaY = IntArray(MAX_TOUCHES)
    private val pressures = FloatArray(MAX_TOUCHES)
    private val realId = IntArray(MAX_TOUCHES) { -1 }

    override val x: Int
        get() = touchX[0]
    override val y: Int
        get() = touchY[0]
    override val deltaX: Int
        get() = touchDeltaX[0]
    override val deltaY: Int
        get() = touchDeltaY[0]
    override val isTouching: Boolean
        get() = inputCache.isTouching
    override val justTouched: Boolean
        get() = inputCache.justTouched
    override val pressure: Float
        get() = getPressure(Pointer.POINTER1)
    override val currentEventTime: Long
        get() = inputCache.currentEventTime
    override val axisLeftX: Float
        get() = getGamepadJoystickXDistance(GameStick.LEFT)
    override val axisLeftY: Float
        get() = getGamepadJoystickYDistance(GameStick.LEFT)
    override val axisRightX: Float
        get() = getGamepadJoystickXDistance(GameStick.RIGHT)
    override val axisRightY: Float
        get() = getGamepadJoystickYDistance(GameStick.RIGHT)

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val action = event.action and MotionEvent.ACTION_MASK
        var pointerIndex =
            (event.action and MotionEvent.ACTION_POINTER_INDEX_MASK) shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
        var pointerId = event.getPointerId(pointerIndex)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val actualIdx = getFreePointerIndex()
                if (actualIdx >= MAX_TOUCHES) return false
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                realId[actualIdx] = pointerId
                touchX[actualIdx] = x.toInt()
                touchY[actualIdx] = y.toInt()
                touchDeltaX[actualIdx] = 0
                touchDeltaY[actualIdx] = 0
                pressures[actualIdx] = event.getPressure(pointerIndex)
                inputCache.onTouchDown(x, y, Pointer.cache[actualIdx])
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_OUTSIDE,
            MotionEvent.ACTION_CANCEL -> {
                val actualIdx = lookUpPointerIndex(pointerId)
                if (actualIdx == -1 || actualIdx >= MAX_TOUCHES) return false
                realId[actualIdx] = -1
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                touchX[actualIdx] = x.toInt()
                touchY[actualIdx] = y.toInt()
                touchDeltaX[actualIdx] = 0
                touchDeltaY[actualIdx] = 0
                pressures[actualIdx] = 0f
                inputCache.onTouchUp(x, y, Pointer.cache[actualIdx])
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerCount = event.pointerCount
                for (i in 0 until pointerCount) {
                    pointerIndex = i
                    pointerId = event.getPointerId(pointerIndex)
                    val actualIdx = lookUpPointerIndex(pointerId)
                    if (actualIdx == -1 || actualIdx >= MAX_TOUCHES) continue
                    val x = event.getX(pointerIndex)
                    val y = event.getY(pointerIndex)
                    touchDeltaX[actualIdx] = (x - touchX[actualIdx]).toInt()
                    touchDeltaY[actualIdx] = (y - touchY[actualIdx]).toInt()
                    touchX[actualIdx] = x.toInt()
                    touchY[actualIdx] = y.toInt()
                    pressures[actualIdx] = event.getPressure(pointerIndex)
                    inputCache.onMove(x, y, Pointer.cache[actualIdx])
                }
            }
        }
        return true
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> inputCache.onKeyDown(keyCode.getKey)
            KeyEvent.ACTION_UP -> {
                inputCache.onKeyUp(keyCode.getKey)
                inputCache.onCharTyped(event.unicodeChar.toChar())
            }
        }

        return catchKeys.contains(keyCode.getKey)
    }

    fun update() {
        inputCache.processEvents(inputProcessors)
    }

    fun reset() {
        inputCache.reset()
    }

    private fun getFreePointerIndex() = realId.indexOfFirst { it == -1 }

    private fun lookUpPointerIndex(pointerId: Int) = realId.firstOrNull { it == pointerId } ?: -1

    override fun getX(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) x else touchX[pointer.index]
    }

    override fun getY(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) y else touchY[pointer.index]
    }

    override fun getDeltaX(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) deltaX else touchDeltaX[pointer.index]
    }

    override fun getDeltaY(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) deltaY else touchDeltaY[pointer.index]
    }

    override fun isJustTouched(pointer: Pointer): Boolean {
        return inputCache.isJustTouched(pointer)
    }

    override fun isTouching(pointer: Pointer): Boolean {
        return inputCache.isTouching(pointer)
    }

    override fun isTouching(totalPointers: Int): Boolean {
        return inputCache.isTouching(totalPointers)
    }

    override fun isTouchJustReleased(pointer: Pointer): Boolean {
        return inputCache.isTouchJustReleased(pointer)
    }

    override fun getPressure(pointer: Pointer): Float {
        return if (isJustTouched(pointer)) pressures[pointer.index] else 0f
    }

    override fun isKeyJustPressed(key: Key): Boolean {
        return inputCache.isKeyJustPressed(key)
    }

    override fun isKeyPressed(key: Key): Boolean {
        return inputCache.isKeyPressed(key)
    }

    override fun isKeyJustReleased(key: Key): Boolean {
        return inputCache.isKeyJustReleased(key)
    }

    override fun isGamepadButtonJustPressed(button: GameButton, gamepad: Int): Boolean {
        return inputCache.isGamepadButtonJustPressed(button, gamepad)
    }

    override fun isGamepadButtonPressed(button: GameButton, gamepad: Int): Boolean {
        return inputCache.isGamepadButtonPressed(button, gamepad)
    }

    override fun isGamepadButtonJustReleased(button: GameButton, gamepad: Int): Boolean {
        return inputCache.isGamepadButtonJustReleased(button, gamepad)
    }

    override fun getGamepadButtonPressure(button: GameButton, gamepad: Int): Float {
        return if (connectedGamepads.isNotEmpty()) gamepads[gamepad][button] else 0f
    }

    override fun getGamepadJoystickDistance(stick: GameStick, gamepad: Int): Point {
        return if (connectedGamepads.isNotEmpty()) gamepads[gamepad][stick] else Point.ZERO
    }

    override fun getGamepadJoystickXDistance(stick: GameStick, gamepad: Int): Float {
        return if (connectedGamepads.isNotEmpty()) gamepads[gamepad].getX(stick) else 0f
    }

    override fun getGamepadJoystickYDistance(stick: GameStick, gamepad: Int): Float {
        return if (connectedGamepads.isNotEmpty()) gamepads[gamepad].getY(stick) else 0f
    }

    override fun setCursorPosition(x: Int, y: Int) = Unit

    override fun addInputProcessor(processor: InputProcessor) {
        _inputProcessors += processor
    }

    override fun removeInputProcessor(processor: InputProcessor) {
        _inputProcessors -= processor
    }

    @SuppressLint("MissingPermission")
    override fun vibrate(duration: Duration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    duration.inWholeMilliseconds,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(duration.inWholeMilliseconds)
        }
    }

    @SuppressLint("MissingPermission")
    override fun cancelVibrate() {
        vibrator.cancel()
    }

    override fun showSoftKeyboard() {
        val view = graphics.surfaceView ?: return
        val manager = androidCtx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        KtScope.launch(Dispatchers.Main) {
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
            manager.showSoftInput(view, 0)
        }
    }

    override fun hideSoftKeyboard() {

        val view = graphics.surfaceView ?: return
        val manager = androidCtx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        KtScope.launch(Dispatchers.Main) {
            manager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    companion object {
        private val MAX_TOUCHES = Pointer.cache.size
    }
}