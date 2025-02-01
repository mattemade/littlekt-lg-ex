package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.util.seconds
import net.mattemade.bossrush.ARENA_RADIUS
import net.mattemade.bossrush.Assets
import kotlin.time.Duration

class Collectible(
    override val position: MutableVec2f = MutableVec2f(),
    val direction: MutableVec2f = MutableVec2f(),
    var elevation: Float = 2f,
    var elevationImpulse: Float = 35f,
    var gravity: Float = 100f,
    val assets: Assets,
): TemporaryDepthRenderableObject {

    private val shadowRadii = Vec2f(3f, 1.5f)
    private val tempVec2f = MutableVec2f()
    var static: Boolean = false
    var collected: Boolean = false

    override val solidRadius: Float = 6f

    override fun update(dt: Duration): Boolean {
        tempVec2f.set(0f, 0f)

        if (!static) {
            elevation += elevationImpulse * dt.seconds
            if (elevation < 0f) {
                static = true
            } else {
                elevationImpulse -= gravity * dt.seconds
                tempVec2f.set(direction).scale(dt.seconds)
            }
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

    override fun startDisappearing() {
        // TODO: disappear once texture is there
        collected = true
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        batch.draw(assets.texture.collectible, x = position.x - assets.texture.collectible.width/2f, y = position.y - assets.texture.collectible.height - elevation)
        //shapeRenderer.filledCircle(x = position.x, y = position.y - 2f - elevation, radius = 2f, color = Color.GREEN.toFloatBits())
    }

    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(
            position,
            radius = shadowRadii.x,
            color = Color.BLACK.toFloatBits()
        )
    }

}