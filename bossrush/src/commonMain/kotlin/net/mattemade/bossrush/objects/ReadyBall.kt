package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.util.seconds
import kotlin.math.sin
import kotlin.time.Duration

class ReadyBall(
    override val position: MutableVec2f = MutableVec2f(),
): TemporaryDepthRenderableObject {

    private val shadowRadii = Vec2f(2f, 1f)
    private val tempVec2f = MutableVec2f()

    override val solidRadius: Float = 6f
    var elevation = 0f
    private var time = 0f

    override fun update(dt: Duration): Boolean {
        tempVec2f.set(0f, 0f)
        time += dt.seconds
        elevation = 14f + 2f * sin(time * 10)
        return true
    }

    override fun displace(displacement: Vec2f) {
        //position.add(displacement)
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(x = position.x, y = position.y - elevation, radius = 2f, color = Color.BLUE.toFloatBits())
    }

    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledEllipse(position, shadowRadii, innerColor = Color.BLUE.toFloatBits(), outerColor = Color.BLACK.toFloatBits())
    }

}