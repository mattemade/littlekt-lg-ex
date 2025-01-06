package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.util.seconds
import kotlin.time.Duration

class Projectile(
    val position: MutableVec2f = MutableVec2f(),
    val direction: MutableVec2f = MutableVec2f()
) {

    var timeToLive = 10f

    fun update(dt: Duration) {
        timeToLive -= dt.seconds
        position.add(direction)
    }

    fun render(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(center = position, radius = 5f, color = Color.YELLOW.toFloatBits())
    }

}