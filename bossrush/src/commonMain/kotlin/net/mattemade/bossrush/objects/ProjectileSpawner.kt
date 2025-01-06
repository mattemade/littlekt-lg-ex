package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.geom.degrees
import com.littlekt.math.geom.times
import com.littlekt.util.seconds
import net.mattemade.bossrush.player.Player
import kotlin.time.Duration

class ProjectileSpawner(
    private val player: Player,
    private val spawn: (Projectile) -> Unit
) {

    private var difficulty = 1
    private var nextParticleIn = 1f
    private val tempVec2f = MutableVec2f()

    fun update(dt: Duration) {
        nextParticleIn -= dt.seconds
        while (nextParticleIn <= 0f) {
            difficulty++
            nextParticleIn += maxOf(1f - (difficulty + 20) / (difficulty + 30).toFloat(), 0.2f)

            val shots = minOf(2 * (difficulty - 2) + 1, 80)
            val deltaAngle = (180f / shots).degrees
            val startAngle = (-shots / 2).toFloat() * deltaAngle

            tempVec2f.set(0f, 0f).add(player.position).setLength(5f).rotate(startAngle)
            for (i in 0 until shots) {
                spawn(Projectile(
                    direction = MutableVec2f(tempVec2f)
                ))
                tempVec2f.rotate(deltaAngle)
            }


        }
    }

    fun render(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(x = 0f, y = 0f, radius = 40f, color = Color.YELLOW.toFloatBits())
    }

}