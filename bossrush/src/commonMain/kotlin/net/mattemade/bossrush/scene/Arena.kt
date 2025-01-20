package net.mattemade.bossrush.scene

import com.littlekt.Context
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.Vec2f
import com.littlekt.math.clamp
import com.littlekt.math.geom.radians
import com.littlekt.math.isFuzzyZero
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.math.minimalRotation
import net.mattemade.bossrush.objects.TextureParticles
import net.mattemade.bossrush.shader.ParticleShader
import net.mattemade.utils.math.fill
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Duration

class Arena(
    var rotation: Float = 0f,
    private val assets: Assets,
    private val context: Context,
    private val shader: ParticleShader
) {

    private val textureSize = Vec2f(assets.texture.arena.width.toFloat(), assets.texture.arena.height.toFloat())
    private val startPosition = Vec2f(-assets.texture.arena.width / 2f, -assets.texture.arena.height / 2f)
    private val tempVec2f = MutableVec2f()

    var angularVelocity: Float = 0f
    private var turningToZeroAction: (() -> Unit)? = null
    private var deactivated: Boolean = false


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
            println("rotation: $rotation, ${(rotation - PI2_F)}")
            if (rotation.isFuzzyZero(eps = 0.01f) || (rotation - PI2_F).isFuzzyZero(eps = 0.01f)) {
                println("EXECUTE!!!")
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
    }

    fun turnToZero(actionOnceDone: () -> Unit) {
        turningToZeroAction = actionOnceDone
    }

    fun startDisappearing() {
        disappearing = true
    }
}