package net.mattemade.bossrush.input

import com.littlekt.Context
import com.littlekt.input.InputMapController
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.util.milliseconds
import net.mattemade.bossrush.NO_ROTATION
import net.mattemade.bossrush.math.minimalRotation
import kotlin.time.Duration

class GameInput(private val context: Context, private val input: InputMapController<ControllerInput>) {

    val previousMovement = MutableVec2f()
    val movement = MutableVec2f()
    val cursorPosition = MutableVec2f()
    var previousRotation: Float = 0f
    var rotation: Float = 0f
        set(value) {
            field = value
        }
    //var absoluteRotation: Float = -PI2_F // for the intro swirl
    var previousDRotation: Float = 0f
    var dRotation: Float = 0f
    var cursorCentered: Boolean = false

    fun update(dt: Duration) {
        val gamepadSwingHorizontal = input.axis(ControllerInput.SWING_HORIZONTAL)
        val gamepadSwingVertical = input.axis(ControllerInput.SWING_VERTICAL)

        if (gamepadSwingHorizontal != 0f || gamepadSwingVertical != 0f) {
            cursorPosition.set(gamepadSwingHorizontal, gamepadSwingVertical).scale(20f)
        } else {
            cursorPosition.add(context.input.deltaX / 4f, context.input.deltaY / 4f)
        }
        val length = cursorPosition.length()
        if (length > 20f) {
            cursorPosition.setLength(20f)
        }

        if (length < 4f) {
            rotation = previousRotation
            dRotation = 0f
            cursorCentered = true
        } else {
            previousRotation = rotation
            previousDRotation = dRotation
            rotation = cursorPosition.angleTo(NO_ROTATION).radians
            dRotation = minimalRotation(previousRotation, rotation)
            if (cursorCentered) {
                previousRotation = rotation
                dRotation = 0f
                previousDRotation = 0f
                cursorCentered = false
            }

            //absoluteRotation += dRotation
        }

        previousMovement.set(movement)
        movement
            .set(input.axis(ControllerInput.MOVE_HORIZONTAL), input.axis(ControllerInput.MOVE_VERTICAL))
            .limit(1f)
            .scale(dt.milliseconds / 20f)
        if (input.down(ControllerInput.SLOW_MODIFIER)) {
            movement.scale(0.5f)
        }
    }

    fun shouldUseItem(): Boolean = input.released(ControllerInput.PLACE_TRAP)

    fun shouldChargeItem(): Boolean = input.down(ControllerInput.PLACE_TRAP)

    private fun MutableVec2f.limit(maxLength: Float): MutableVec2f =
        if (length() > maxLength) {
            setLength(maxLength)
        } else {
            this
        }
}