package net.mattemade.bossrush.objects

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.PI_F
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.radians
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import kotlin.time.Duration

class Swing(private val playerPosition: MutableVec2f, private val playerRotation: Float, private val clockwise: Boolean, private val assets: Assets) :
    TemporaryDepthRenderableObject {

    init {
        println("swinging $clockwise at: $playerRotation")
    }

    private var timeToLive = 0.25f

    private val texture = assets.texture.swingLight
    override val position: Vec2f = MutableVec2f().apply {
        set(60f - texture.height, -texture.width / 2f)
        rotate((playerRotation).radians)
        add(playerPosition)
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
            rotation = (playerRotation + PI_F/2f).radians,
            flipX = clockwise,
        )
    }
}