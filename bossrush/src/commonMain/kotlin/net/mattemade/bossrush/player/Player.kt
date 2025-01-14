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
import net.mattemade.bossrush.ARENA_RADIUS
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.NO_ROTATION
import net.mattemade.bossrush.SWING_ANGLE
import net.mattemade.bossrush.input.GameInput
import net.mattemade.bossrush.math.minimalRotation
import net.mattemade.bossrush.objects.TemporaryDepthRenderableObject
import kotlin.math.abs
import kotlin.time.Duration

class Player(
    private val context: Context,
    private val input: InputMapController<GameInput>,
    private val assets: Assets,
    private val placingTrap: (seconds: Float, released: Boolean) -> Unit,
    private val swing: (angle: Float, clockwise: Boolean, powerful: Boolean) -> Unit,
) : TemporaryDepthRenderableObject {

    var resources: Int = 1000
    var hearts: Int = 5
    var previousPosition = MutableVec2f(0f, 100f)
    override var position = MutableVec2f(0f, 100f)
    var previousRotationStopOrPivot = 0f
    var swingingForMillis = 0f
    var rotation = 0f
    var relativeRacketPosition = MutableVec2f(0f, 0f)
    var racketPosition = MutableVec2f(0f, 0f)
    var trappedForSeconds = 0f
    var damagedForSeconds = 0f
    private val damageColor = Color.RED.toFloatBits()
    override val solidRadius: Float
        get() = 4f

    var isReadyToSwing = true
    var previousDRotation = 0f

    private val shadowRadii = Vec2f(10f, 5f)
    private var circleInFront: Boolean = false
    private var circleRotation: Float = 0f
    private val textureSlices = assets.texture.robot.slice(sliceWidth = 40, sliceHeight = 34)[0]
    private val textureSequence = listOf(1, 0, 7, 6, 4, 3, 2).map { textureSlices[it] }
    private val positions = textureSequence.size/* * 2*/

    //private val positions2 = assets.texture.sequence2.size * 2
    private val radInSegment = PI2_F / positions
    //private val radInSegment2 = PI2_F / positions2

    private val debugCharacterColor = MutableColor(Color.BLUE).withAlpha(0.2f).toFloatBits()
    private val debugRacketColor = MutableColor(Color.WHITE).withAlpha(0.2f).toFloatBits()

    private var placingTrapForSeconds = 0f
    private val bumpingDirection = MutableVec2f()
    private var bumpingForSeconds = 0f

    private val tempVec2f = MutableVec2f()

    private fun MutableVec2f.limit(maxLength: Float): MutableVec2f =
        if (length() > maxLength) {
            setLength(maxLength)
        } else {
            this
        }

    override fun update(dt: Duration): Boolean {
        previousPosition.set(position)
        if (damagedForSeconds > 0f) {
            damagedForSeconds = maxOf(0f, damagedForSeconds - dt.seconds)
        }
        if (trappedForSeconds > 0f) {
            trappedForSeconds = maxOf(0f, trappedForSeconds - dt.seconds)
            if (trappedForSeconds > 0f) {
                return true
            }
        }

        val gamepadSwingHorizontal = input.axis(GameInput.SWING_HORIZONTAL)
        val gamepadSwingVertical = input.axis(GameInput.SWING_VERTICAL)

        val previousCircleRotation = relativeRacketPosition.angleTo(NO_ROTATION).radians
        if (gamepadSwingHorizontal != 0f || gamepadSwingVertical != 0f) {
            relativeRacketPosition.set(gamepadSwingHorizontal, gamepadSwingVertical).scale(20f)
        } else {
            relativeRacketPosition.add(context.input.deltaX / 4f, context.input.deltaY / 4f)
        }
        if (relativeRacketPosition.length() > 20f) {
            relativeRacketPosition.setLength(20f)
        }
        racketPosition.set(position).add(relativeRacketPosition)
        circleRotation = relativeRacketPosition.angleTo(NO_ROTATION).radians

        val dRotation = minimalRotation(previousCircleRotation, circleRotation)
        //val dRotation = context.input.deltaX / 200f
        rotation += dRotation

        val swingingAngleDifference = previousRotationStopOrPivot - rotation
        val absSwingingAngleDifference = abs(swingingAngleDifference)
        swingingForMillis += dt.milliseconds
        if (isReadyToSwing && absSwingingAngleDifference >= PI_F * SWING_ANGLE) {
            val sign = swingingAngleDifference / absSwingingAngleDifference
            swing(
                previousRotationStopOrPivot % PI2_F - sign * PI_F * SWING_ANGLE,
                swingingAngleDifference > 0f,
                swingingForMillis <= 120f
            )
            previousRotationStopOrPivot = rotation
            isReadyToSwing = false
            swingingForMillis = 0f
        }

        val rotationSpeed = abs(dRotation) / dt.milliseconds // slow 0.02 light 0.04 quick
        val pivoting = dRotation > 0f && previousDRotation <= 0f || dRotation <= 0f && previousDRotation > 0f
        if ((rotationSpeed > 0f && rotationSpeed < 0.005f) || pivoting) {
            previousRotationStopOrPivot = rotation
            isReadyToSwing = true
            swingingForMillis = 0f
        }
        previousDRotation = dRotation

        tempVec2f
            .set(input.axis(GameInput.MOVE_HORIZONTAL), input.axis(GameInput.MOVE_VERTICAL))
            .limit(1f)
            .scale(dt.milliseconds / 20f)
        if (input.down(GameInput.SLOW_MODIFIER)) {
            tempVec2f.scale(0.5f)
        }
        position.add(tempVec2f)

        if (bumpingForSeconds > 0f) {
            bumpingForSeconds = maxOf(0f, bumpingForSeconds - dt.seconds)
            tempVec2f.set(bumpingDirection).scale(dt.seconds)
            position.add(tempVec2f)
        }
        if (position.length() > ARENA_RADIUS) {
            position.setLength(ARENA_RADIUS)
        }

        /*circleRotation = (rotation % PI2_F + PI2_F) % PI2_F
        circleInFront = circleRotation < PI_F
        racketPosition.set(
            position.x + 20f * cos(circleRotation),
            position.y + 20f * sin(circleRotation)
        )*/

        if (input.released(GameInput.PLACE_TRAP)) {
            placingTrap(placingTrapForSeconds, true)
            placingTrapForSeconds = 0f
        } else if (input.down(GameInput.PLACE_TRAP) && !input.pressed(GameInput.PLACE_TRAP)) {
            placingTrapForSeconds += dt.seconds
            placingTrap(placingTrapForSeconds, false)
            isReadyToSwing = false
            swingingForMillis = 0f
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

        val segment = ((circleRotation / radInSegment).floorToInt() % positions + positions) % positions
        batch.draw(
            textureSequence[segment],
            x = position.x - 20f,
            y = position.y - 30f,
            width = 40f,
            height = 34f,
            flipX = false,
            colorBits = if (damagedForSeconds > 0f) damageColor * damagedForSeconds else batch.colorBits,
        )

        if (trappedForSeconds > 0f) {
            for (i in 0..2) {
                tempVec2f.set(10f, 0f)
                    .rotate((trappedForSeconds * 3f + i * PI2_F / 3f).radians)
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

        shapeRenderer.filledCircle(
            x = racketPosition.x,
            y = racketPosition.y,
            radius = 3f,
            color = debugRacketColor
        )

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

    fun damaged() {
        if (damagedForSeconds == 0f) {
            damagedForSeconds = 0.75f
            hearts--
            if (hearts == 0) {
                // TODO game over, spin the time
            }
        }
    }

    fun bump(from: Vec2f) {
        bumpingDirection.set(position).subtract(from).setLength(100f)
        bumpingForSeconds = 0.25f
    }

}