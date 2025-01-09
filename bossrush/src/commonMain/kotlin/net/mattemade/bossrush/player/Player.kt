package net.mattemade.bossrush.player

import com.littlekt.Context
import com.littlekt.graphics.Color
import com.littlekt.graphics.MutableColor
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.input.InputMapController
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.PI_F
import com.littlekt.math.Vec2f
import com.littlekt.math.floorToInt
import com.littlekt.math.geom.radians
import com.littlekt.util.milliseconds
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.input.GameInput
import net.mattemade.bossrush.objects.TemporaryDepthRenderableObject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration

class Player(
    private val context: Context,
    private val input: InputMapController<GameInput>,
    private val assets: Assets,
    private val placingTrap: (seconds: Float, released: Boolean) -> Unit,
): TemporaryDepthRenderableObject {

    var previousPosition = MutableVec2f(0f, 100f)
    override var position = MutableVec2f(0f, 100f)
    var rotation = 0f
    var racketPosition = MutableVec2f(0f, 0f)
    var trappedForSeconds = 0f
    override val solidRadius: Float
        get() = 4f
    private val shadowRadii = Vec2f(4f, 2f)
    private var circleInFront: Boolean = false
    private var circleRotation: Float = 0f
    private val textureSequence = listOf(assets.texture.downRight, assets.texture.right, assets.texture.upRight)
    private val positions = textureSequence.size * 2

    //private val positions2 = assets.texture.sequence2.size * 2
    private val radInSegment = PI2_F / positions
    //private val radInSegment2 = PI2_F / positions2

    private val debugCharacterColor = MutableColor(Color.BLUE).withAlpha(0.2f).toFloatBits()
    private val debugRacketColor = MutableColor(Color.WHITE).withAlpha(0.2f).toFloatBits()

    private var placingTrapForSeconds = 0f

    private val tempVec2f = MutableVec2f()

    private fun MutableVec2f.limit(maxLength: Float): MutableVec2f =
        if (length() > maxLength) {
            setLength(maxLength)
        } else {
            this
        }

    override fun update(dt: Duration): Boolean {
        previousPosition.set(position)
        if (trappedForSeconds > 0f) {
            trappedForSeconds -= dt.seconds
            return true
        }
        rotation += context.input.deltaX / 200f

        tempVec2f
            .set(input.axis(GameInput.MOVE_HORIZONTAL), input.axis(GameInput.MOVE_VERTICAL))
            .limit(1f)
            .scale(dt.milliseconds / 20f)

        // TODO: check if we bumped into any obstacles on the way, and change the position accordingly
        position.add(tempVec2f)

        circleRotation = (rotation % PI2_F + PI2_F) % PI2_F
        circleInFront = circleRotation < PI_F
        racketPosition.set(
            position.x + 20f * cos(circleRotation),
            position.y + 20f * sin(circleRotation)
        )

        if (input.released(GameInput.PLACE_TRAP)) {
            placingTrap(placingTrapForSeconds, true)
            placingTrapForSeconds = 0f
        } else if (input.down(GameInput.PLACE_TRAP) && !input.pressed(GameInput.PLACE_TRAP)) {
            placingTrapForSeconds += dt.seconds
            placingTrap(placingTrapForSeconds, false)
        }
        return true
    }

    override fun displace(displacement: Vec2f) {
        // TODO: check if we bumped into any obstacles on the way, and change the position accordingly
        // TODO: but should not be possible, as all the obstacles are equaly displaced ¦3
        position.add(displacement)
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        /*if (!circleInFront) drawCircle(shapeRenderer)
        shapeRenderer.filledRectangle(
            x = position.x - 25f,
            y = position.y - 50f,
            width = 50f,
            height = 100f,
            color = debugCharacterColor
        )
        if (circleInFront) drawCircle(shapeRenderer)*/

        val segment = (((circleRotation - PI_F / 2f) / radInSegment).floorToInt() % positions + positions) % positions
        val index = if (segment >= textureSequence.size) (positions - 1 - segment) else segment
        batch.draw(
            textureSequence[index],
            x = position.x - 16f,
            y = position.y - 30f,
            width = 32f,
            height = 32f,
            flipX = segment < textureSequence.size
        )

        if (trappedForSeconds > 0f) {
            for (i in 0..2) {
                tempVec2f.set(10f, 0f)
                    .rotate((trappedForSeconds*3f + i * PI2_F / 3f).radians)
                    .scale(1f, 0.5f)
                    .add(-4f, -4f) // offset the middle of the texture
                    .add(position) // offset into character position
                batch.draw(
                    assets.texture.littleStar,
                    x = tempVec2f.x,
                    y = tempVec2f.y - 30f,
                    width = 8f,
                    height = 8f,
                )
            }
        }

        if (placingTrapForSeconds > 0f) {
            shapeRenderer.filledCircle(
                x = racketPosition.x,
                y = racketPosition.y,
                radius = 20f,
                color = debugRacketColor
            )
        }


        /*val segment2 = (((circleRotation - PI_F/2f) / radInSegment2).floorToInt() % positions2 + positions2) % positions2
        val index2 = if (segment2 >= assets.texture.sequence2.size) (positions2 - 1 - segment2) else segment2
        batch.draw(assets.texture.sequence2[index2], x = position.x - 250f, y = position.y-50f, width = 100f, height= 100f, flipX = segment2 < assets.texture.sequence2.size)*/


    }

    private fun drawCircle(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(
            x = racketPosition.x,
            y = racketPosition.y,
            radius = 20f,
            color = debugRacketColor
        )
    }


    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledEllipse(
            position,
            shadowRadii,
            innerColor = Color.BLUE.toFloatBits(),
            outerColor = Color.BLACK.toFloatBits()
        )
    }

    fun trapped() {
        trappedForSeconds = 2f
    }

}