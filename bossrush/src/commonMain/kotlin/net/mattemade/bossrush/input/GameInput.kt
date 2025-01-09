package net.mattemade.bossrush.input

import com.littlekt.Context
import com.littlekt.input.GameAxis
import com.littlekt.input.GameButton
import com.littlekt.input.InputMapController
import com.littlekt.input.Key
import com.littlekt.input.Pointer

enum class GameInput {
    MOVE_LEFT,
    MOVE_RIGHT,
    MOVE_HORIZONTAL, // mapped from LEFT and RIGHT based on axis

    MOVE_UP,
    MOVE_DOWN,
    MOVE_VERTICAL, // mapped from UP and DOWN based on axis

    SWING, // either right stick or mouse, how to map that?

    SLOW_MODIFIER,
    FAST_MODIFIER,
    PLACE_TRAP,

    ANY,
}

fun Context.bindInputs(): InputMapController<GameInput> =
    InputMapController<GameInput>(input).apply {
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
            GameInput.MOVE_RIGHT,
            listOf(Key.D, Key.ARROW_RIGHT).any(),
            axes = listOf(GameAxis.LX),
            buttons = listOf(GameButton.RIGHT).any()
        )
        addBinding(
            GameInput.MOVE_LEFT,
            listOf(Key.A, Key.ARROW_LEFT).any(),
            axes = listOf(GameAxis.LX),
            buttons = listOf(GameButton.LEFT).any()
        )
        addAxis(GameInput.MOVE_HORIZONTAL, GameInput.MOVE_RIGHT, GameInput.MOVE_LEFT)

        addBinding(
            GameInput.MOVE_UP,
            listOf(Key.W, Key.ARROW_UP).any(),
            axes = listOf(GameAxis.LY),
            buttons = listOf(GameButton.UP).any()
        )
        addBinding(
            GameInput.MOVE_DOWN,
            listOf(Key.S, Key.ARROW_DOWN).any(),
            axes = listOf(GameAxis.LY),
            buttons = listOf(GameButton.DOWN).any()
        )
        addAxis(GameInput.MOVE_VERTICAL, GameInput.MOVE_DOWN, GameInput.MOVE_UP)

        addBinding(
            GameInput.PLACE_TRAP,
            listOf(Key.J, Key.X).any().action(),
            buttons = listOf(GameButton.XBOX_A, GameButton.XBOX_Y).any().action(),
            // JS configuration is messed up, and right/middle mouse buttons have different codes in browsers
            pointers = listOf(Pointer.MOUSE_RIGHT, Pointer.MOUSE_MIDDLE),
        )
        addBinding(
            GameInput.SLOW_MODIFIER,
            listOf(Key.SHIFT_LEFT).any().action(),
            buttons = listOf(GameButton.XBOX_X, GameButton.XBOX_B).any().action()
        )

        addBinding(
            GameInput.FAST_MODIFIER,
            listOf(Key.SPACE).any(),
            buttons = listOf(GameButton.START).any(),
            pointers = listOf(Pointer.MOUSE_LEFT), // TODO: it will break touch screen compatibility
        )
        //addBinding(GameInput.SELECT, listOf(Key.R), buttons = listOf(GameButton.SELECT))

        addBinding(GameInput.ANY, anyKey, buttons = anyButton)
        //addBinding(GameInput.ANY_ACTION, anyActionKey, buttons = anyActionButton)

        mode = InputMapController.InputMode.GAMEPAD

        input.addInputProcessor(this)
    }