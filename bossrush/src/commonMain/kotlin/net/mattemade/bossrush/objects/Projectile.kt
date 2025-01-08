package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.util.milliseconds
import com.littlekt.util.seconds
import kotlin.time.Duration

class Projectile(
    val position: MutableVec2f = MutableVec2f(),
    val direction: MutableVec2f = MutableVec2f()
) {

    var timeToLive = 10f
    private val shadowRadii = Vec2f(2f, 1f)
    private val tempVec2f = MutableVec2f()

    fun update(dt: Duration) {
        timeToLive -= dt.seconds
        tempVec2f.set(direction).scale(dt.milliseconds / 100f)
        position.add(tempVec2f)
    }

    fun render(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(x = position.x, y = position.y - 3f, radius = 2f, color = Color.YELLOW.toFloatBits())
    }


    fun renderShadow(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledEllipse(position, shadowRadii, innerColor = Color.BLUE.toFloatBits(), outerColor = Color.BLACK.toFloatBits())
    }

}