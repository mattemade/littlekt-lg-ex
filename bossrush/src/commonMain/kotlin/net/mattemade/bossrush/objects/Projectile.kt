package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.SOUND_VOLUME
import net.mattemade.bossrush.math.rotateTowards
import kotlin.time.Duration

class Projectile(
    val assets: Assets,
    val texture: TextureSlice,
    override val position: MutableVec2f = MutableVec2f(),
    val direction: MutableVec2f = MutableVec2f(),
    override var solidElevation: Float = 6f,
    var elevationRate: Float = 0f,
    var gravity: Float = 0f,
    override val solidRadius: Float = texture.width / 2f,
    val scale: Float = 1f,
    val isReversible: Boolean = true,
    val isBomb: Boolean = true,
    var timeToLive: Float = 0f,
    val onPlayerImpact: (Projectile) -> Unit = {},
    val onSolidImpact: (Projectile) -> Unit,
) : TemporaryDepthRenderableObject {

    private val shadowRadii = MutableVec2f(texture.width * scale / 2f, texture.height * scale / 4f)
    private val tempVec2f = MutableVec2f()
    var target: (() -> Vec2f?)? = null
    var targetElevation: (() -> Float)? = null
    var angularSpeedScale = 1f
    var canDamageBoss: Boolean = false

    override fun update(dt: Duration): Boolean {
        if (timeToLive > 0f) {
            timeToLive -= dt.seconds
            if (timeToLive <= 0f) {
                return false
            }
        }

        targetElevation?.invoke()?.let { targetElevation ->
            solidElevation += (targetElevation - solidElevation) * dt.seconds
        } ?: run {
            elevationRate -= gravity * dt.seconds
            solidElevation += elevationRate * dt.seconds
            if (solidElevation <= 0f) {
                assets.sound.projectileLand.play(volume = SOUND_VOLUME, positionX = position.x, positionY = position.y)
                onSolidImpact(this)
                return false
            }
        }
        target?.invoke()?.let { targetPosition ->
            tempVec2f.set(targetPosition).subtract(position)
            direction.rotateTowards(tempVec2f, limit = dt.seconds * angularSpeedScale)
        }
        tempVec2f.set(direction).scale(dt.seconds)
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
        )
    }

    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledEllipse(
            position,
            shadowRadii.set(texture.width / 2f, texture.height / 4f).scale(scale).scale(if (solidElevation < 10f) 1f else if (solidElevation > 400f) 0f else 1f - 1f * (solidElevation - 10f) / (400f - 10f)),
            innerColor = Color.BLUE.toFloatBits(),
            outerColor = Color.BLACK.toFloatBits()
        )
    }
}