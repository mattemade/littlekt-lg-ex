package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI_F
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.radians
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import kotlin.time.Duration

class Swing(
    private val playerPosition: MutableVec2f,
    private val playerRotation: Float,
    private val clockwise: Boolean,
    private val assets: Assets,
    private val powerful: Boolean
) :
    TemporaryDepthRenderableObject {

    private var timeToLive = 0.25f

    private val texture = if (powerful) assets.texture.swingStrong else assets.texture.swingLight
    val hitFrontRadius = 40f
    val hitFrontPosition = MutableVec2f().apply {
        set(0f, 0f)
        rotate(playerRotation.radians)
        add(playerPosition)
    }.toVec2()
    val hitBackRadius = 64f
    val hitBackPosition = MutableVec2f().apply {
        set(-58f, 0f)
        rotate(playerRotation.radians)
        add(playerPosition)
    }.toVec2()
    override val position: Vec2f = MutableVec2f().apply {
        set(-texture.width/2f, -texture.height / 2f - 20f)
        rotate((playerRotation + PI_F/2f).radians)
        add(playerPosition)
    }.toVec2()

    override fun update(dt: Duration): Boolean {
        timeToLive -= dt.seconds
        return timeToLive > 0f
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
//        shapeRenderer.filledCircle(center = hitFrontPosition, radius = hitFrontRadius, color = Color.RED.toFloatBits())
//        shapeRenderer.filledCircle(center = hitBackPosition, radius = hitBackRadius, color = Color.BLACK.toFloatBits())
        batch.draw(
            texture,
            x = position.x,
            y = position.y,
            width = texture.width.toFloat(),
            height = texture.height.toFloat(),
            rotation = (playerRotation + PI_F/2f).radians,
            flipX = clockwise,
        )
    }
}