package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.degrees
import com.littlekt.math.geom.radians
import com.littlekt.math.geom.times
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.player.Player
import kotlin.time.Duration

class TestBoss(
    private val player: Player,
    private val assets: Assets,
    private val spawn: (Projectile) -> Unit
): TemporaryDepthRenderableObject {

    override val position = MutableVec2f(0f, -100f)
    private var trappedForSeconds = 0f
    private val shadowRadii = Vec2f(10f, 5f)

    override val solidRadius: Float
        get() = 8f
    private var difficulty = 1
    private var nextParticleIn = 1f
    private val tempVec2f = MutableVec2f()

    override fun update(dt: Duration): Boolean {
        if (trappedForSeconds > 0f) {
            trappedForSeconds = maxOf(0f, trappedForSeconds - dt.seconds)
            if (trappedForSeconds > 0f) {
                return true
            }
        }
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
        return true
    }

    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledEllipse(position, shadowRadii, innerColor = Color.BLUE.toFloatBits(), outerColor = Color.BLACK.toFloatBits())
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(x = position.x, y = position.y - 10f, radius = 10f, color = Color.YELLOW.toFloatBits())

        if (trappedForSeconds > 0f) {
            for (i in 0..2) {
                tempVec2f.set(10f, 0f)
                    .rotate((trappedForSeconds*3f + i * PI2_F / 3f).radians)
                    .scale(1f, 0.5f)
                    .add(-4f, -4f) // offset the middle of the texture
                    .add(position) // offset into character position
                batch.draw(
                    assets.texture.littleStar,
                    x = tempVec2f.x,
                    y = tempVec2f.y - 30f,
                    width = 8f,
                    height = 8f,
                )
            }
        }
    }

    fun trapped() {
        trappedForSeconds += 5f
    }

}