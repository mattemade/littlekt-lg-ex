package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.util.milliseconds
import com.littlekt.util.seconds
import kotlin.time.Duration

class Projectile(
    override val position: MutableVec2f = MutableVec2f(),
    val direction: MutableVec2f = MutableVec2f()
): TemporaryDepthRenderableObject {

    var timeToLive = 10f
    private val shadowRadii = Vec2f(2f, 1f)
    private val tempVec2f = MutableVec2f()

    override fun update(dt: Duration): Boolean {
        timeToLive -= dt.seconds
        tempVec2f.set(direction).scale(dt.milliseconds / 100f)
        position.add(tempVec2f)
        return timeToLive > 0f
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(x = position.x, y = position.y - 8f, radius = 2f, color = Color.YELLOW.toFloatBits())
    }

    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledEllipse(position, shadowRadii, innerColor = Color.BLUE.toFloatBits(), outerColor = Color.BLACK.toFloatBits())
    }

}