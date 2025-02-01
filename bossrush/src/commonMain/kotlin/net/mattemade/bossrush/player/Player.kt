package net.mattemade.bossrush.player

import com.littlekt.Context
import com.littlekt.graphics.Color
import com.littlekt.graphics.MutableColor
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
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
import net.mattemade.bossrush.SOUND_VOLUME
import net.mattemade.bossrush.SWING_ANGLE
import net.mattemade.bossrush.input.GameInput
import net.mattemade.bossrush.math.minimalRotation
import net.mattemade.bossrush.objects.TemporaryDepthRenderableObject
import net.mattemade.bossrush.objects.TextureParticles
import net.mattemade.bossrush.shader.ParticleShader
import net.mattemade.utils.math.fill
import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random
import kotlin.time.Duration

class Player(
    private val context: Context,
    private val input: GameInput,
    private val assets: Assets,
    private val shader: ParticleShader,
    private val placingTrap: (seconds: Float, released: Boolean) -> Unit,
    private val swing: (angle: Float, clockwise: Boolean, powerful: Boolean) -> Unit,
) : TemporaryDepthRenderableObject {

    var resources: Int = 0
        set(value) {
            field = minOf(value, 20)
        }
    var hearts: Int = 3
    var maxHearts: Int = 3
    override val solidHeight: Float = 30f
    var dizziness: Float = 0f
    var dizzy: Boolean = false
    override var position = MutableVec2f(0f, -80f)
    var previousPosition = position.toMutableVec2()
    var previousRotationStopOrPivot = 0f
    //var swingingForMillis = 0f
    //var rotation = 0f
    //var relativeRacketPosition = MutableVec2f(0f, 0f)
    var racketPosition = MutableVec2f(0f, 0f)
    var starTime = 0f
    var trappedForSeconds = 0f
    var damagedForSeconds = 0f
    private val damageColor = Color.RED.toFloatBits()
    override val solidRadius: Float
        get() = 4f

    var isReadyToSwing = true
    var lastInputRotation = 0f
    var stayingStillFor = 0f
    //var previousDRotation = 0f

    private val disappearingFor = 10000f
    private fun createParticles(texture: TextureSlice): TextureParticles {
        val width = texture.width
        val widthFloat = texture.width.toFloat()
        val height = texture.height
        val heightFloat = height.toFloat()
        val halfWidth = width / 2f
        val halfHeight = height / 2f

        return TextureParticles(
            context,
            shader,
            texture,
            position,
            interpolation = 2,
            activeFrom = { x, y -> Random.nextFloat() * 300f + (height - y) * 100f },
            activeFor = { x, y -> 4000f },
            timeToLive = disappearingFor,
            setStartColor = { a = 0f },
            setEndColor = { a = 1f },
            setStartPosition = { x, y ->
                fill(-width * 2f + width * 4f * Random.nextFloat(), y - heightFloat * 8f)
            },
            setEndPosition = { x, y ->
                fill(x - halfWidth, y - 30f) // normal rendering offsets
            },
        )
    }
    private var disappear: TextureParticles? = null

    var disappearing: Boolean = false
        set(value) {
            if (!field && value) {
                val segment = ((input.rotation / radInSegment).floorToInt() % positions + positions) % positions
                disappear = createParticles(textureSequence[segment])
                disappear?.addToTime(disappearingFor)
            }
            field = value
        }

    private val shadowRadii = MutableVec2f(10f, 5f)
    private var circleInFront: Boolean = false
    //private var circleRotation: Float = 0f
    private val textureSlices = assets.texture.robot.slice(sliceWidth = assets.texture.robot.width / 8, sliceHeight = assets.texture.robot.height)[0]
    private val textureSequence = listOf(1, 0, 7, 6, 4, 3, 2).map { textureSlices[it] }
    private val positions = textureSequence.size/* * 2*/

    //private val positions2 = assets.texture.sequence2.size * 2
    private val radInSegment = PI2_F / positions
    //private val radInSegment2 = PI2_F / positions2

    private val debugCharacterColor = MutableColor(Color.BLUE).withAlpha(0.2f).toFloatBits()
    private val debugRacketColor = MutableColor(Color.WHITE).withAlpha(0.8f).toFloatBits()

    var placingTrapForSeconds = 0f
        set(value) {
            field = minOf(value, if (resources >= 10) 2.99f else if (resources >= 5) 1.99f else if (resources >= 2f) 0.99f else 0f)
        }
    private val bumpingDirection = MutableVec2f()
    private var bumpingForSeconds = 0f

    private val tempVec2f = MutableVec2f()

    override fun update(dt: Duration): Boolean {
        starTime = (starTime + dt.seconds) % PI2_F

        racketPosition.set(position).add(input.cursorPosition)

        disappear?.let {
            it.update(-dt)
            shadowRadii.set(10f, 5f).scale(it.liveFactor)
            return it.liveFactor > 0f
        }
        previousPosition.set(position)
        if (damagedForSeconds > 0f) {
            damagedForSeconds = maxOf(0f, damagedForSeconds - dt.seconds)
        }

        if (!dizzy && dizziness >= 5f) {
            dizzy = true
            dizziness = 5f
        }
        if (dizziness > 0f) {
            dizziness = maxOf(0f, dizziness - if (dizzy) dt.seconds * 2f else dt.seconds)
            if (dizziness == 0f) {
                dizzy = false
            }
        }
        if (dizzy) {
            return true
        }

        if (trappedForSeconds > 0f) {
            dizzy = false
            dizziness = 0f
            trappedForSeconds = maxOf(0f, trappedForSeconds - dt.seconds)
            if (trappedForSeconds > 0f) {
                return true
            }
        }

        val rotation = input.rotation
        lastInputRotation = rotation

        val rotationSpeed = abs(input.dRotation) / dt.milliseconds // slow 0.02 light 0.04 quick
        if (rotationSpeed == 0f) {
            stayingStillFor += dt.seconds
            if (stayingStillFor > 0.125f) {
                previousRotationStopOrPivot = input.previousRotation
                isReadyToSwing = true
            }
            //isReadyToSwing = false
        } else {
            val pivoting = sign(input.previousMeaningfulDRotation) * sign(input.dRotation) < 0f
            if (rotationSpeed < 0.005f || pivoting) {
                previousRotationStopOrPivot = input.previousRotation
                isReadyToSwing = true
                //swingingForMillis = 0f
            }
        }

        val swingingAngleDifference = minimalRotation(previousRotationStopOrPivot, rotation)
        val absSwingingAngleDifference = abs(swingingAngleDifference)
        //swingingForMillis += dt.milliseconds
        if (isReadyToSwing && absSwingingAngleDifference >= PI_F * SWING_ANGLE && input.canSwing()) {
            val movementLength = input.movement.length() / dt.seconds
            val movementAngle = input.movement.angleTo(NO_ROTATION).radians
            val swingRotation = rotation - input.dRotation/2f
            val angleDiff = minimalRotation(swingRotation, movementAngle)
            val swingingTowardsMoving = sign(swingingAngleDifference) == sign(angleDiff)
            val absAngleDiff = abs(angleDiff)
            val strongBlow = movementLength > 35f && swingingTowardsMoving && absAngleDiff < PI_F*0.75f
            dizziness += 1f
            swing(
                swingRotation,
                swingingAngleDifference < 0f,
                strongBlow
            )
            previousRotationStopOrPivot = rotation
            isReadyToSwing = false
//            swingingForMillis = 0f
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
            //swingingForMillis = 0f
        }
        return true
    }

    override fun displace(displacement: Vec2f) {
        // TODO: check if we bumped into any obstacles on the way, and change the position accordingly
        // TODO: but should not be possible, as all the obstacles are equaly displaced ¦3
        position.add(displacement)
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        disappear?.let {
            it.render(batch, shapeRenderer)
            return
        }

        if (racketPosition.y <= position.y) {
            shapeRenderer.filledCircle(
                x = racketPosition.x,
                y = racketPosition.y,
                radius = 3f,
                color = debugRacketColor
            )
        }

        val segment = ((lastInputRotation / radInSegment).floorToInt() % positions + positions) % positions
        val slice = textureSequence[segment]
        batch.draw(
            slice,
            x = position.x - slice.width/2f,
            y = position.y - slice.height * 0.8f,
            width = slice.width.toFloat(),
            height = slice.height.toFloat(),
            flipX = false,
            colorBits = if (damagedForSeconds > 0f) damageColor * damagedForSeconds else batch.colorBits,
        )

        val starCount = if (trappedForSeconds > 0f) (trappedForSeconds*2f).floorToInt() + 1 else if (dizzy) ((dizziness+1f).floorToInt()) else dizziness.floorToInt()
        if (starCount > 0) {
            for (i in 0 until starCount) {
                tempVec2f.set(10f, 0f)
                    .rotate((starTime * 3f + i * PI2_F / 5).radians)
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
        shapeRenderer.filledCircle(
            position,
            radius = shadowRadii.x,
            color = Color.BLACK.toFloatBits()
        )
    }

    fun trapped() {
        trappedForSeconds = 2f
        placingTrapForSeconds = 0f
        bumpingDirection.set(0f, 0f)
    }

    fun damaged() {
        if (hearts > 0 && damagedForSeconds == 0f) {
            assets.sound.playerHit.play(volume = SOUND_VOLUME, positionX = position.x, positionY = position.y)
            damagedForSeconds = 0.75f
            hearts--
            if (hearts == 0) {
                // TODO game over, spin the time
                trapped()
            }
        }
    }

    override fun startDisappearing() {
        disappearing = true
    }

    fun bumpFrom(from: Vec2f) {
        bumpingDirection.set(position).subtract(from).setLength(100f)
        bumpingForSeconds = 0.25f
    }

}