package net.mattemade.bossrush.objects

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import kotlin.time.Duration

class Trap(override val position: MutableVec2f, private val assets: Assets): TemporaryDepthRenderableObject {

    private val texture = assets.texture.trap
    private val textureActivated = assets.texture.trapActivated
    var activatedTimeToLive = -1f

    override val solidRadius: Float
        get() = 4f

    override fun displace(displacement: Vec2f) {
        position.add(displacement)
    }

    override fun update(dt: Duration): Boolean {
        if (activatedTimeToLive > 0f) {
            activatedTimeToLive -= dt.seconds
            if (activatedTimeToLive <= 0f) {
                return false
            }
        }
        return true
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        batch.draw(
            if (activatedTimeToLive > 0f) textureActivated else texture,
            x = position.x - 16f,
            y = position.y - 24f,
            width = 32f,
            height = 32f,
        )
    }

    fun activate() {
        activatedTimeToLive = 1f
    }
}
