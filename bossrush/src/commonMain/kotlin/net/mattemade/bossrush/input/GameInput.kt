package net.mattemade.bossrush.input

import com.littlekt.Context
import com.littlekt.input.InputMapController
import com.littlekt.math.MutableVec2f
import com.littlekt.util.milliseconds
import net.mattemade.bossrush.MOUSE_SENS
import net.mattemade.bossrush.NO_ROTATION
import net.mattemade.bossrush.SHOULD_USE_SWING_MODIFIER
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
    var previousMeaningfulDRotation: Float = 0f
    var dRotation: Float = 0f
    var cursorCentered: Boolean = false
    var gamepadInput: Boolean = false

    fun update(dt: Duration) {
        val gamepadSwingHorizontal = input.axis(ControllerInput.SWING_HORIZONTAL)
        val gamepadSwingVertical = input.axis(ControllerInput.SWING_VERTICAL)


        gamepadInput = if (gamepadSwingHorizontal != 0f || gamepadSwingVertical != 0f) {
            cursorPosition.set(gamepadSwingHorizontal, gamepadSwingVertical).scale(20f)
            true
        } else {
            cursorPosition.add(context.input.deltaX * MOUSE_SENS / 4f, context.input.deltaY * MOUSE_SENS / 4f)
            false
        }
        val length = cursorPosition.length()
        if (length > 20f) {
            cursorPosition.setLength(20f)
        }

        val deadzone = if (gamepadInput) 0f else 4f
        if (length < deadzone) {
            rotation = previousRotation
            dRotation = 0f
            cursorCentered = true
        } else {
            previousRotation = rotation
            previousDRotation = dRotation
            if (dRotation != 0f) {
                previousMeaningfulDRotation = dRotation
            }
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

    fun canSwing(): Boolean = !SHOULD_USE_SWING_MODIFIER || input.down(ControllerInput.SWING_MODIFIER)

    fun shouldUseItem(): Boolean =
        if (SHOULD_USE_SWING_MODIFIER) input.released(ControllerInput.PLACE_TRAP) && !input.released(ControllerInput.SWING_MODIFIER) else input.released(
            ControllerInput.PLACE_TRAP
        )

    fun shouldChargeItem(): Boolean =
        if (SHOULD_USE_SWING_MODIFIER) input.down(ControllerInput.PLACE_TRAP) && !input.down(ControllerInput.SWING_MODIFIER) else input.down(
            ControllerInput.PLACE_TRAP
        )

    private fun MutableVec2f.limit(maxLength: Float): MutableVec2f =
        if (length() > maxLength) {
            setLength(maxLength)
        } else {
            this
        }
}