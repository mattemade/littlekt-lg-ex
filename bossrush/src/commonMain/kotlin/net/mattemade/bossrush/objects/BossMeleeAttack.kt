package net.mattemade.bossrush.objects

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI_F
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.radians
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import kotlin.time.Duration

class BossMeleeAttack(
    private val bossPosition: Vec2f,
    private val hitRotation: Float,
    private val clockwise: Boolean,
    private val assets: Assets,
) :
    TemporaryDepthRenderableObject {


    private var timeToLive = 0.25f

    private val texture = assets.texture.swingStrong
    override val position: Vec2f = MutableVec2f().apply {
        set(60f - texture.height, -texture.width / 2f)
        rotate(hitRotation.radians)
        add(bossPosition)
    }.toVec2()

    override fun update(dt: Duration): Boolean {
        timeToLive -= dt.seconds
        return timeToLive > 0f
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        batch.draw(
            texture,
            x = position.x,
            y = position.y - 8f,
            width = texture.width.toFloat(),
            height = texture.height.toFloat(),
            rotation = (hitRotation + PI_F / 2f).radians,
            flipX = clockwise,
        )
    }
}