package net.mattemade.bossrush.input

import com.littlekt.Context
import com.littlekt.input.GameAxis
import com.littlekt.input.GameButton
import com.littlekt.input.InputMapController
import com.littlekt.input.Key
import com.littlekt.input.Pointer

enum class ControllerInput {
    MOVE_LEFT,
    MOVE_RIGHT,
    MOVE_HORIZONTAL, // mapped from LEFT and RIGHT based on axis

    MOVE_UP,
    MOVE_DOWN,
    MOVE_VERTICAL, // mapped from UP and DOWN based on axis

    // right stick only, mouse is controlled separately
    SWING_LEFT,
    SWING_RIGHT,
    SWING_HORIZONTAL,

    SWING_UP,
    SWING_DOWN,
    SWING_VERTICAL,

    SLOW_MODIFIER,
    //FAST_MODIFIER,
    PLACE_TRAP,

    ANY,
}

fun Context.bindInputs(): InputMapController<ControllerInput> =
    InputMapController<ControllerInput>(input).apply {
        // the 'A' and 'left arrow' keys and the 'x-axis of the left stick' with trigger the 'MOVE_LEFT' input type
        val anyKey = mutableListOf<Key>()
        val anyActionKey = mutableListOf<Key>()
        val anyButton = mutableListOf<GameButton>()
        val anyActionButton = mutableListOf<GameButton>()
        fun List<Key>.any(): List<Key> = this.also { anyKey.addAll(this) }
        fun List<Key>.action(): List<Key> = this.also { anyActionKey.addAll(this) }
        fun List<GameButton>.any(): List<GameButton> = this.also { anyButton.addAll(this) }
        fun List<GameButton>.action(): List<GameButton> = this.also { anyActionButton.addAll(this) }

        addBinding(
            ControllerInput.MOVE_RIGHT,
            listOf(Key.D, Key.ARROW_RIGHT).any(),
            axes = listOf(GameAxis.LX),
            buttons = listOf(GameButton.RIGHT).any()
        )
        addBinding(
            ControllerInput.MOVE_LEFT,
            listOf(Key.A, Key.ARROW_LEFT).any(),
            axes = listOf(GameAxis.LX),
            buttons = listOf(GameButton.LEFT).any()
        )
        addAxis(ControllerInput.MOVE_HORIZONTAL, ControllerInput.MOVE_RIGHT, ControllerInput.MOVE_LEFT)

        addBinding(
            ControllerInput.MOVE_UP,
            listOf(Key.W, Key.ARROW_UP).any(),
            axes = listOf(GameAxis.LY),
            buttons = listOf(GameButton.UP).any()
        )
        addBinding(
            ControllerInput.MOVE_DOWN,
            listOf(Key.S, Key.ARROW_DOWN).any(),
            axes = listOf(GameAxis.LY),
            buttons = listOf(GameButton.DOWN).any()
        )
        addAxis(ControllerInput.MOVE_VERTICAL, ControllerInput.MOVE_DOWN, ControllerInput.MOVE_UP)

        addBinding(ControllerInput.SWING_RIGHT, axes = listOf(GameAxis.RX),)
        addBinding(ControllerInput.SWING_LEFT, axes = listOf(GameAxis.RX),)
        addAxis(ControllerInput.SWING_HORIZONTAL, ControllerInput.SWING_RIGHT, ControllerInput.SWING_LEFT)

        addBinding(ControllerInput.SWING_UP, axes = listOf(GameAxis.RY),)
        addBinding(ControllerInput.SWING_DOWN, axes = listOf(GameAxis.RY),)
        addAxis(ControllerInput.SWING_VERTICAL, ControllerInput.SWING_DOWN, ControllerInput.SWING_UP)

        addBinding(
            ControllerInput.PLACE_TRAP,
            buttons = listOf(GameButton.R1).any().action(),
            // JS configuration is messed up, and right/middle mouse buttons have different codes in browsers
            pointers = listOf(Pointer.MOUSE_LEFT, Pointer.MOUSE_RIGHT, Pointer.MOUSE_MIDDLE),
        )
        addBinding(
            ControllerInput.SLOW_MODIFIER,
            listOf(Key.SHIFT_LEFT, Key.CTRL_RIGHT).any().action(),
            buttons = listOf(GameButton.L1).any().action()
        )

        addBinding(ControllerInput.ANY, anyKey, buttons = anyButton)

        mode = InputMapController.InputMode.GAMEPAD

        input.addInputProcessor(this)
    }