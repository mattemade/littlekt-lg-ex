package net.mattemade.bossrush.scene

import com.littlekt.Context
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.gl.State
import com.littlekt.graphics.toFloatBits
import com.littlekt.graphics.util.BlendMode
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.PI_F
import com.littlekt.math.Vec2f
import com.littlekt.math.clamp
import com.littlekt.math.geom.radians
import com.littlekt.math.interpolate
import com.littlekt.math.isFuzzyZero
import com.littlekt.math.smoothStep
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.SOUND_VOLUME
import net.mattemade.bossrush.math.minimalRotation
import net.mattemade.bossrush.objects.TextureParticles
import net.mattemade.bossrush.shader.ParticleShader
import net.mattemade.utils.math.fill
import net.mattemade.utils.render.PixelRender
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Duration

class Arena(
    var rotation: Float = 0f,
    private val assets: Assets,
    private val context: Context,
    private val shader: ParticleShader,
    private val bossLeft: () -> Float,
) {

    private val textureSize = Vec2f(assets.texture.arena.width.toFloat(), assets.texture.arena.height.toFloat())
    private val startPosition = Vec2f(-assets.texture.arena.width / 2f, -assets.texture.arena.height / 2f)
    private val tempVec2f = MutableVec2f()
    var currentHour = 0

    var angularVelocity: Float = 0f
        set(value) {
            field = value
            assets.sound.arenaRotating.volume = smoothStep(0f, 1f, abs(value)) * SOUND_VOLUME / 20f / 2f// abs(value/ 2f).pow(2)
        }
    private var turningToZeroAction: (() -> Unit)? = null
    private var deactivated: Boolean = false

    private val clockMask = Color.WHITE.toMutableColor().apply {
        a = 0f
    }

    init {
        context.vfs.launch {
            assets.sound.arenaRotating.play(volume = 0f, loop = true)
        }
    }

    private var lastHour = 0f

    private val clockTextureSize = Vec2f(assets.texture.giantClock.width.toFloat(), assets.texture.giantClock.height.toFloat())
    private val clockStartPosition = Vec2f(-assets.texture.giantClock.width / 2f, -assets.texture.giantClock.height / 2f)
    private val clockRender = PixelRender(
        context,
        clockTextureSize.x.toInt(),
        clockTextureSize.y.toInt(),
        clockTextureSize.x.toInt(),
        clockTextureSize.y.toInt(),
        { dt, camera -> },
        { dt, camera, batch ->

            batch.draw(assets.texture.giantClock, x = clockStartPosition.x, y = clockStartPosition.y, width = clockTextureSize.x, height = clockTextureSize.y)
            //val bossNumber = 1
            //val totalMinutes = currentHour * 60 + (1 - bossProgress()) * 60// absoluteTime// * 20f
            val minutes: Float// = movingMinute % 60f//(1f - bossProgress()) * 60f
            val hours: Float// = /*currentHour +*/ movingMinute / 60f

            /*if (currentHour == 5) {
                minutes = 0f
                hours = 0f
            } else*/ /*if (currentHour == 4) {
                minutes = (-movingMinute) % 60f
                hours = 8f - movingMinute / 60f
            } else*/ //{
                minutes = movingMinute % 60f
                hours = /*currentHour +*/ movingMinute / 60f
            //}

            if (hours != lastHour) {
                println("hour: $hours")
                lastHour = hours
            }

            if (hours >= 4 || currentHour >= 4) putHour(batch, assets.texture.giantClockX, 0f)
            if (hours >= 1 || currentHour >= 4) putHour(batch, assets.texture.giantClockI, PI_F / 2f)
            if (hours >= 2 || currentHour >= 4) putHour(batch, assets.texture.giantClockII, PI_F)
            if (hours >= 3 || currentHour >= 4) putHour(batch, assets.texture.giantClockV, PI2_F * 0.75f)

            val minutesRotation = minutes * PI2_F / 60f// + PI_F
            val hoursRotation = hours * PI2_F / 4f// - PI_F/2f
            tempVec2f.set(clockStartPosition).rotate(hoursRotation.radians)
            batch.draw(
                assets.texture.giantClockHour,
                x = tempVec2f.x,
                y = tempVec2f.y,
                width = clockTextureSize.x,
                height = clockTextureSize.y,
                rotation = hoursRotation.radians,
                //colorBits = clockColor
            )
            tempVec2f.set(clockStartPosition).rotate(minutesRotation.radians)
            batch.draw(
                assets.texture.giantClockMinute,
                x = tempVec2f.x,
                y = tempVec2f.y,
                width = clockTextureSize.x,
                height = clockTextureSize.y,
                rotation = minutesRotation.radians,
                //colorBits = clockColor
            )
        },
        clear = true,
    )
    private val clockTexture = clockRender.texture

    private val disappear = TextureParticles(
        context,
        shader,
        assets.texture.arena,
        startPosition.toMutableVec2(),
        interpolation = 2,
        //activeFrom = { x, y -> Random.nextFloat()*300f + /*(textureSize.y -*/ y/*)*/*200f },
        activeFrom = { x, y ->
            val length = tempVec2f.set(x.toFloat(), y.toFloat()).add(startPosition).length()
            Random.nextFloat() * 1000f + minOf(length, 140f - length) * 200f
            //Random.nextFloat() * 1000f + maxOf(length, 80f - length) * 500f
        },
        activeFor = { x, y -> 8000f },
        timeToLive = 18000f,
        setStartColor = { a = 1f },
        setEndColor = { a = 0f },
        /*setStartPosition = { x, y ->
            fill(x.toFloat(), y.toFloat()) // normal rendering offsets
        },*/
        setEndPosition = { x, y ->
            fill(x - 2f + 4f * Random.nextFloat(), y - textureSize.y)
        },
    )

    var disappearing: Boolean = false
    var previousProgress: Float = 0f
    var showingClockFor: Float = 0f
    val maxShowingClockFor: Float = 4f

    var movingMinuteFrom = 0f
    var movingMinute = 0f
    var movingMinuteTo = 0f
    val maxMovingClockFor: Float = maxShowingClockFor/2f

    fun updateClock(dt: Duration) {
        val progress = 1f - bossLeft()
        if (previousProgress == 1f && progress == 0f || previousProgress == 0f && progress == 1f) {
            previousProgress = progress
            if (currentHour == 5) {
                movingMinuteFrom = 0f
                movingMinute = 0f
                movingMinuteTo = 0f
            } else if (currentHour == 4) {
                movingMinuteFrom = currentHour * 60f - progress*4f * 60f
                movingMinute = currentHour * 60f - progress*4f * 60f
                movingMinuteTo = currentHour * 60f - progress*4f * 60f
            } else {
                movingMinuteFrom = currentHour * 60f + progress * 60f
                movingMinute = currentHour * 60f + progress * 60f
                movingMinuteTo = currentHour * 60f + progress * 60f
            }
        } else if (previousProgress != progress) {
            movingMinuteFrom = movingMinute
            if (currentHour == 5) {
                movingMinuteTo = 0f
            } else if (currentHour == 4) {
                movingMinuteTo = currentHour * 60f - minOf(240f, progress * 4f * 60f)
            } else {
                movingMinuteTo = currentHour * 60f + minOf(60f, progress * 60f)
            }
            showingClockFor = if (progress == 1f) maxShowingClockFor + 3f else maxShowingClockFor
            previousProgress = progress
        }
        //clockMask.a = (clockMask.a + dt.seconds / 2f) % 1f
        if (showingClockFor > 0f) {
            showingClockFor = maxOf(0f, showingClockFor - dt.seconds)
            val passed = maxShowingClockFor - showingClockFor
            clockMask.a = if (passed < maxMovingClockFor) 1f else (showingClockFor / (maxShowingClockFor - maxMovingClockFor)).pow(2)
            val minuteFactor = minOf(1f, passed / maxMovingClockFor)
            movingMinute = smoothStep(0f, 1f, minuteFactor).interpolate(movingMinuteFrom, movingMinuteTo)// minuteFactor.interpolate(movingMinuteFrom, movingMinuteTo)

        }

        clockRender.render(dt)
    }

    fun update(dt: Duration) {
        if (disappearing) {
            disappear.update(dt)
        }
        if (deactivated) {
            angularVelocity = 0f
            return
        }
        if (turningToZeroAction != null) {
            rotation = (rotation % PI2_F + PI2_F) % PI2_F
            angularVelocity = minimalRotation(rotation, 0f)
            rotation += dt.seconds * angularVelocity
            rotation = rotation.clamp(0f, PI2_F)
            if (rotation.isFuzzyZero(eps = 0.01f) || (rotation - PI2_F).isFuzzyZero(eps = 0.01f)) {
                rotation = 0f
                angularVelocity = 0f
                turningToZeroAction?.invoke()
                deactivated = true
            }
            return
        }
        rotation += dt.seconds * angularVelocity
        if (angularVelocity > 0f) {
            angularVelocity = maxOf(0f, angularVelocity - dt.seconds / 8f)
        } else {
            angularVelocity = minOf(0f, angularVelocity + dt.seconds / 8f)
        }
    }

    fun adjustVelocity(previousPosition: Vec2f, position: Vec2f, scale: Float) {
        val angle = previousPosition.angleTo(position).radians
        if (!angle.isFuzzyZero()) {
            val distanceBasedFadeOut = sqrt(previousPosition.length() * position.length()) / 10f
            // TODO: account for dt!!! otherwise it will break on different FPS
            if (angle > 0f) {
                angularVelocity = minOf(1f, angularVelocity + angle * scale * distanceBasedFadeOut)
            } else {
                angularVelocity = maxOf(-1f, angularVelocity + angle * scale * distanceBasedFadeOut)
            }
        }
    }

    fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        if (disappearing) {
            disappear.render(batch, shapeRenderer)
            return
        }


        val angle = rotation.radians
        tempVec2f.set(startPosition).rotate(angle)
        batch.draw(
            assets.texture.arena,
            x = tempVec2f.x,
            y = tempVec2f.y,
            width = textureSize.x,
            height = textureSize.y,
            rotation = angle
        )
        /*batch.draw(
            assets.texture.arenaForeground,
            x = clockStartPosition.x,
            y = clockStartPosition.y,
            width = clockTextureSize.x,
            height = clockTextureSize.y,
        )*/
        //shapeRenderer.filledRectangle(x = 0f, y = 0f, width = 200f, height = 200f, color = Color.BLACK.toFloatBits())


        //val clockColor = clockMask.toFloatBits()
        val oldBits = batch.colorBits
        batch.colorBits = clockMask.toFloatBits()

        context.gl.enable(State.BLEND)
        //batch.setBlendFunctionSeparate(BlendFactor.SRC_COLOR, BlendFactor.ONE_MINUS_DST_COLOR, BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA)
        batch.setBlendFunction(BlendMode.Add)
        batch.draw(
            clockTexture,
            x = clockStartPosition.x,
            y = clockStartPosition.y,
            width = clockTextureSize.x,
            height = clockTextureSize.y,
            flipY = true
        )
        batch.setToPreviousBlendFunction()
        context.gl.disable(State.BLEND)
        batch.colorBits = oldBits
    }

    private fun putHour(batch: Batch, texture: TextureSlice, angle: Float) {
        tempVec2f.set(0f, -84f).subtract(texture.width / 2f, texture.height / 2f).rotate((angle/* - PI_F/2f*/).radians)
        batch.draw(
            texture,
            x = tempVec2f.x,
            y = tempVec2f.y,
            width = texture.width.toFloat(),
            height = texture.height.toFloat(),
            rotation = angle.radians,
        )
    }

    fun turnToZero(actionOnceDone: () -> Unit) {
        turningToZeroAction = actionOnceDone
    }

    fun startDisappearing() {
        disappearing = true
    }

    fun setClockFactor(factor: Float) {
        clockMask.a = minOf(1f, factor * factor)
    }
    fun fadeClockOut() {
        //previousProgress = 0f
        movingMinuteFrom = movingMinute
        movingMinuteTo = movingMinute
        showingClockFor = 2f + maxShowingClockFor// - maxMovingClockFor
    }
}