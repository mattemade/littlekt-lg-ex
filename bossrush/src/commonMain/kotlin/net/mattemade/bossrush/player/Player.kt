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
import net.mattemade.bossrush.input.ControllerInput
import net.mattemade.bossrush.input.GameInput
import net.mattemade.bossrush.math.minimalRotation
import net.mattemade.bossrush.objects.TemporaryDepthRenderableObject
import kotlin.math.abs
import kotlin.math.sign
import kotlin.time.Duration

class Player(
    private val context: Context,
    private val input: GameInput,//InputMapController<ControllerInput>,
    private val assets: Assets,
    private val placingTrap: (seconds: Float, released: Boolean) -> Unit,
    private val swing: (angle: Float, clockwise: Boolean, powerful: Boolean) -> Unit,
) : TemporaryDepthRenderableObject {

    var resources: Int = 1000
    var hearts: Int = 5
    var dizziness: Float = 0f
    var dizzy: Boolean = false
    var previousPosition = MutableVec2f(0f, 100f)
    override var position = MutableVec2f(0f, 100f)
    var previousRotationStopOrPivot = 0f
    var swingingForMillis = 0f
    //var rotation = 0f
    //var relativeRacketPosition = MutableVec2f(0f, 0f)
    var racketPosition = MutableVec2f(0f, 0f)
    var trappedForSeconds = 0f
    var damagedForSeconds = 0f
    private val damageColor = Color.RED.toFloatBits()
    override val solidRadius: Float
        get() = 4f

    var isReadyToSwing = true
    //var previousDRotation = 0f

    private val shadowRadii = Vec2f(10f, 5f)
    private var circleInFront: Boolean = false
    //private var circleRotation: Float = 0f
    private val textureSlices = assets.texture.robot.slice(sliceWidth = 40, sliceHeight = 34)[0]
    private val textureSequence = listOf(1, 0, 7, 6, 4, 3, 2).map { textureSlices[it] }
    private val positions = textureSequence.size/* * 2*/

    //private val positions2 = assets.texture.sequence2.size * 2
    private val radInSegment = PI2_F / positions
    //private val radInSegment2 = PI2_F / positions2

    private val debugCharacterColor = MutableColor(Color.BLUE).withAlpha(0.2f).toFloatBits()
    private val debugRacketColor = MutableColor(Color.WHITE).withAlpha(0.8f).toFloatBits()

    private var placingTrapForSeconds = 0f
    private val bumpingDirection = MutableVec2f()
    private var bumpingForSeconds = 0f

    private val tempVec2f = MutableVec2f()

    override fun update(dt: Duration): Boolean {
        previousPosition.set(position)
        if (damagedForSeconds > 0f) {
            damagedForSeconds = maxOf(0f, damagedForSeconds - dt.seconds)
        }

        if (!dizzy && dizziness >= 5f) {
            dizzy = true
            dizziness = 5f
        }
        if (dizziness > 0f) {
            dizziness = maxOf(0f, dizziness - dt.seconds)
        }
        if (dizzy) {
            return true
        }

        if (trappedForSeconds > 0f) {
            dizziness = 0f
            trappedForSeconds = maxOf(0f, trappedForSeconds - dt.seconds)
            if (trappedForSeconds > 0f) {
                return true
            }
        }


        racketPosition.set(position).add(input.cursorPosition)

        val rotation = input.rotation
        val swingingAngleDifference = minimalRotation(previousRotationStopOrPivot, rotation)
        val absSwingingAngleDifference = abs(swingingAngleDifference)
        swingingForMillis += dt.milliseconds
        if (isReadyToSwing && absSwingingAngleDifference >= PI_F * SWING_ANGLE) {
            swing(
                rotation,
                swingingAngleDifference < 0f,
                swingingForMillis <= 120f
            )
            previousRotationStopOrPivot = rotation
            isReadyToSwing = false
            swingingForMillis = 0f
        }

        val rotationSpeed = abs(input.dRotation) / dt.milliseconds // slow 0.02 light 0.04 quick
        if (rotationSpeed == 0f) {
            isReadyToSwing = false
        } else {
            val pivoting = sign(input.previousDRotation) * sign(input.dRotation) < 0f
            if (rotationSpeed < 0.005f || pivoting) {
                previousRotationStopOrPivot = rotation
                isReadyToSwing = true
                swingingForMillis = 0f
            }
        }

        position.add(input.movement)

        if (bumpingForSeconds > 0f) {
            bumpingForSeconds = maxOf(0f, bumpingForSeconds - dt.seconds)
            tempVec2f.set(bumpingDirection).scale(dt.seconds)
            position.add(tempVec2f)
        }
        if (position.length() > ARENA_RADIUS) {
            position.setLength(ARENA_RADIUS)
        }

        if (input.shouldUseItem()) {
            placingTrap(placingTrapForSeconds, true)
            placingTrapForSeconds = 0f
        } else if (input.shouldChargeItem()) {
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
        if (racketPosition.y <= position.y) {
            shapeRenderer.filledCircle(
                x = racketPosition.x,
                y = racketPosition.y,
                radius = 3f,
                color = debugRacketColor
            )
        }

        val segment = ((input.rotation / radInSegment).floorToInt() % positions + positions) % positions
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

        if (racketPosition.y > position.y) {
            shapeRenderer.filledCircle(
                x = racketPosition.x,
                y = racketPosition.y,
                radius = 3f,
                color = debugRacketColor
            )
        }

        if (placingTrapForSeconds > 0f) {
            shapeRenderer.filledCircle(
                x = racketPosition.x,
                y = racketPosition.y,
                radius = 20f,
                color = debugRacketColor
            )
        }
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