package net.mattemade.bossrush.player

import com.littlekt.Context
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.input.InputMapController
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.PI_F
import com.littlekt.util.milliseconds
import net.mattemade.bossrush.input.GameInput
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration

class Player(private val context: Context, val input: InputMapController<GameInput>) {

    var position = MutableVec2f(0f, 400f)
    var rotation = 0f
    var racketPosition = MutableVec2f(0f, 0f)
    private var circleInFront: Boolean = false

    private val tempVec2f = MutableVec2f()

    private fun MutableVec2f.limit(maxLength: Float): MutableVec2f =
        if (length() > maxLength) {
            setLength(maxLength)
        } else {
            this
        }

    fun update(dt: Duration) {
        rotation += context.input.deltaX / 200f

        tempVec2f
            .set(input.axis(GameInput.MOVE_HORIZONTAL), input.axis(GameInput.MOVE_VERTICAL))
            .limit(1f)
            .scale(dt.milliseconds / 2f)

        position.add(tempVec2f)

        val circleRotation = (rotation % PI2_F + PI2_F) % PI2_F
        circleInFront = circleRotation < PI_F
        racketPosition.set(
            position.x + 50f * cos(circleRotation),
            position.y + 25f * sin(circleRotation)
        )

    }

    fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        if (!circleInFront) drawCircle(shapeRenderer)
        shapeRenderer.filledRectangle(x = position.x - 25f, y = position.y - 50f, width = 50f, height = 100f, color = Color.RED.toFloatBits())
        if (circleInFront) drawCircle(shapeRenderer)
    }

    private fun drawCircle(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(
            x = racketPosition.x,
            y = racketPosition.y,
            radius = 20f,
            color = Color.WHITE.toFloatBits()
        )
    }

}