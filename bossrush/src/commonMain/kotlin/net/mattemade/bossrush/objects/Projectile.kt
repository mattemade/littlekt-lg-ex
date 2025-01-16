package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.util.seconds
import net.mattemade.bossrush.math.rotateTowards
import kotlin.time.Duration

class Projectile(
    override val position: MutableVec2f = MutableVec2f(),
    val direction: MutableVec2f = MutableVec2f(),
    var elevation: Float = 6f,
    var elevationRate: Float = 0f,
    var gravity: Float = 0f,
    val spawnCollectible: (Projectile) -> Unit
): TemporaryDepthRenderableObject {

    var timeToLive = 10f
    private val shadowRadii = Vec2f(2f, 1f)
    private val tempVec2f = MutableVec2f()
    var target: (() -> Vec2f?)? = null
    var targetElevation: (() -> Float)? = null
    var angularSpeedScale = 1f
    var canDamageBoss: Boolean = false

    override fun update(dt: Duration): Boolean {
        //timeToLive -= dt.seconds

        targetElevation?.invoke()?.let { targetElevation ->
            elevation += (targetElevation - elevation) * dt.seconds
        } ?: run {
            elevationRate -= gravity * dt.seconds
            elevation += elevationRate * dt.seconds
            if (elevation <= 0f) {
                spawnCollectible(this)
                return false
            }
        }
        target?.invoke()?.let { targetPosition ->
            tempVec2f.set(targetPosition).subtract(position)
            direction.rotateTowards(tempVec2f, limit = dt.seconds * angularSpeedScale)
        }
        tempVec2f.set(direction).scale(dt.seconds)
        position.add(tempVec2f)
        return timeToLive > 0f
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(x = position.x, y = position.y - 2f - elevation, radius = 2f, color = Color.YELLOW.toFloatBits())
    }

    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledEllipse(position, shadowRadii, innerColor = Color.BLUE.toFloatBits(), outerColor = Color.BLACK.toFloatBits())
    }

}