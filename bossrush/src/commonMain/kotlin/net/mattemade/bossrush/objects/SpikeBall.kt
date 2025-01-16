package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.Vec2f
import com.littlekt.math.floorToInt
import com.littlekt.math.geom.radians
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.input.GameInput
import net.mattemade.bossrush.player.Player
import net.mattemade.bossrush.scene.Arena
import kotlin.time.Duration

class SpikeBall(
    private val centerPosition: MutableVec2f,
    private val movingRadius: Float = 50f,
    private val angularSpeed: Float = 1f,
    private val assets: Assets,
    private val arena: Arena,
) : TemporaryDepthRenderableObject {

    override val position =  MutableVec2f()
    private var rotation: Float = 0f
    private val texture = assets.texture.spikeBall
    private val shadowRadii = Vec2f(20f, 10f)

    override fun update(dt: Duration): Boolean {
        rotation += angularSpeed * dt.seconds
        position.set(movingRadius, 0f).rotate((rotation/* + arena.rotation*/).radians).add(centerPosition)
        return true
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        batch.draw(
            texture,
            x = position.x - 24f,
            y = position.y - 50f,
            width = 48f,
            height = 48f,
        )
    }

    override fun displace(displacement: Vec2f) {
        position.add(displacement)
    }


    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledEllipse(
            position,
            shadowRadii,
            innerColor = Color.BLUE.toFloatBits(),
            outerColor = Color.BLACK.toFloatBits()
        )
    }

    override val solidRadius: Float = 20f
}