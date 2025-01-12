package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.util.milliseconds
import com.littlekt.util.seconds
import net.mattemade.bossrush.ARENA_RADIUS
import net.mattemade.bossrush.math.rotateTowards
import kotlin.time.Duration

class Collectible(
    override val position: MutableVec2f = MutableVec2f(),
    val direction: MutableVec2f = MutableVec2f(),
    var elevation: Float = 2f,
    var targetElevation: Float = 10f,
): TemporaryDepthRenderableObject {

    private val shadowRadii = Vec2f(2f, 1f)
    private val tempVec2f = MutableVec2f()
    var static: Boolean = false
    var collected: Boolean = false

    override val solidRadius: Float = 6f

    override fun update(dt: Duration): Boolean {
        tempVec2f.set(0f, 0f)
        if (elevation < targetElevation) {
            elevation = minOf(targetElevation, elevation + dt.seconds * 40f)
            if (elevation == targetElevation) {
                targetElevation = 0f
            }
            tempVec2f.set(direction).scale(dt.seconds)
        } else if (elevation > 0f) {
            elevation = maxOf(0f, elevation - dt.seconds * 40f)
            if (elevation > 0f) {
                tempVec2f.set(direction).scale(dt.seconds)
            } else {
                static = true
            }
        } else {
            static = false
        }

        if (!static) {
            position.add(tempVec2f)
            if (position.length() > ARENA_RADIUS) {
                collected = true
            }
        }
        return !collected
    }

    override fun displace(displacement: Vec2f) {
        position.add(displacement)
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(x = position.x, y = position.y - 2f - elevation, radius = 2f, color = Color.GREEN.toFloatBits())
    }

    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledEllipse(position, shadowRadii, innerColor = Color.BLUE.toFloatBits(), outerColor = Color.BLACK.toFloatBits())
    }

}