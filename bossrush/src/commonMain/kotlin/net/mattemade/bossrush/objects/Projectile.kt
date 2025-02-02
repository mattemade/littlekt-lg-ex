package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.Angle
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.FRONT_ROTATION
import net.mattemade.bossrush.NO_ROTATION
import net.mattemade.bossrush.SOUND_VOLUME
import net.mattemade.bossrush.math.rotateTowards
import net.mattemade.bossrush.maybePlay
import kotlin.time.Duration

class Projectile(
    val assets: Assets,
    var texture: TextureSlice,
    override val position: MutableVec2f = MutableVec2f(),
    val direction: MutableVec2f = MutableVec2f(),
    override var solidElevation: Float = 6f,
    var elevationRate: Float = 0f,
    var gravity: Float = 0f,
    override val solidRadius: Float = texture.width / 2f,
    var scale: Float = 1f,
    val isReversible: Boolean = true,
    val isBomb: Boolean = false,
    var timeToLive: Float = 0f,
    val onPlayerImpact: (Projectile) -> Unit = {},
    val onSolidImpact: (Projectile) -> Unit,
    var rotating: Boolean = false,
    var animationSlices: Array<TextureSlice>? = null,
    private val timePerFrame: Float = 0.2f,
) : TemporaryDepthRenderableObject {

    private val previousPosition = position.toMutableVec2()
    private val shadowRadii = MutableVec2f(texture.width * scale / 2f, texture.height * scale / 4f)
    private val tempVec2f = MutableVec2f()
    var target: (() -> Vec2f?)? = null
    var targetElevation: (() -> Float)? = null
    var angularSpeedScale = 1f
    var canDamageBoss: Boolean = false
    var animationStep = 0f
    private var frame = 0

    override fun update(dt: Duration): Boolean {
        if (timeToLive > 0f) {
            timeToLive -= dt.seconds
            if (timeToLive <= 0f) {
                return false
            }
        }

        animationSlices?.let {
            animationStep += dt.seconds
            if (animationStep > timePerFrame) {
                animationStep -= timePerFrame
                frame = (frame + 1) % it.size
                texture = it[frame]
            }
        }

        targetElevation?.invoke()?.let { targetElevation ->
            solidElevation += (targetElevation - solidElevation) * dt.seconds
        } ?: run {
            elevationRate -= gravity * dt.seconds
            solidElevation += elevationRate * dt.seconds
            if (solidElevation <= 0f) {
                assets.sound.projectileLand.maybePlay(position)
                onSolidImpact(this)
                return false
            }
        }
        target?.invoke()?.let { targetPosition ->
            tempVec2f.set(targetPosition).subtract(position)
            direction.rotateTowards(tempVec2f, limit = dt.seconds * angularSpeedScale)
        }
        tempVec2f.set(direction).scale(dt.seconds)
        previousPosition.set(position)
        position.add(tempVec2f)
        return true
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        batch.draw(
            texture,
            x = position.x - texture.width * scale / 2f,
            y = position.y - texture.height * scale / 2f - 2f - solidElevation,
            width = texture.width * scale,
            height = texture.height * scale,
            rotation = if (rotating) tempVec2f.set(previousPosition).subtract(position)
                .angleTo(FRONT_ROTATION) else Angle.ZERO,
        )
    }

    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(
            position,
            radius = shadowRadii.set(texture.width / 2f, texture.height / 4f).scale(scale)
                .scale(if (solidElevation < 10f) 1f else if (solidElevation > 400f) 0f else 1f - 1f * (solidElevation - 10f) / (400f - 10f)).x,
            color = Color.BLACK.toFloatBits()
        )
    }
}