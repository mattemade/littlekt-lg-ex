package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.degrees
import com.littlekt.math.geom.times
import com.littlekt.util.seconds
import net.mattemade.bossrush.player.Player
import kotlin.time.Duration

class TestBoss(
    private val player: Player,
    private val spawn: (Projectile) -> Unit
) {

    val position = MutableVec2f(0f, -100f)
    private val shadowRadii = Vec2f(10f, 5f)

    private var difficulty = 1
    private var nextParticleIn = 1f
    private val tempVec2f = MutableVec2f()

    fun update(dt: Duration) {
        nextParticleIn -= dt.seconds
        while (nextParticleIn <= 0f) {
            //difficulty++
            nextParticleIn += 1f//maxOf(1f - (difficulty + 20) / (difficulty + 30).toFloat(), 0.2f)

            val shots = 1//minOf(2 * (difficulty - 2) + 1, 80)
            val deltaAngle = (180f / shots).degrees
            val startAngle = (-shots / 2).toFloat() * deltaAngle

            tempVec2f.set(player.position).subtract(position).setLength(5f).rotate(startAngle)
            for (i in 0 until shots) {
                spawn(Projectile(
                    position = position.toMutableVec2(),
                    direction = MutableVec2f(tempVec2f)
                ))
                tempVec2f.rotate(deltaAngle)
            }


        }
    }

    fun renderShadow(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledEllipse(position, shadowRadii, innerColor = Color.BLUE.toFloatBits(), outerColor = Color.BLACK.toFloatBits())
    }

    fun render(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(x = position.x, y = position.y - 10f, radius = 10f, color = Color.YELLOW.toFloatBits())
    }

}