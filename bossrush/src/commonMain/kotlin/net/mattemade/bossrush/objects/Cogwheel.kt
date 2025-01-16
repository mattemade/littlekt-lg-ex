package net.mattemade.bossrush.objects

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.Vec2f
import com.littlekt.math.floorToInt
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.input.GameInput
import net.mattemade.bossrush.player.Player
import kotlin.time.Duration

class Cogwheel(
    override val position: MutableVec2f,
    private val player: Player,
    private val input: GameInput,
    private val assets: Assets
) : TemporaryDepthRenderableObject {

    var rotation = 0f
    private val textures = listOf(assets.texture.cogwheelTurn0, assets.texture.cogwheelTurn1, assets.texture.cogwheelTurn2)
    private val segments = listOf(0, 1, 2, 0, 1, 2).map { textures[it] }
    private val positions = segments.size
    private val radInSegment = PI2_F / positions

    override fun update(dt: Duration): Boolean {
        if (player.position.distance(position) < player.solidRadius * 5f + solidRadius) {
            rotation -= input.dRotation
        }
        return true
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        val segment = ((rotation / radInSegment).floorToInt() % positions + positions) % positions
        batch.draw(
            segments[segment],
            x = position.x - 16f,
            y = position.y - 40f,
            width = 32f,
            height = 48f,
        )
    }

    override fun displace(displacement: Vec2f) {
        position.add(displacement)
    }

    override val solidRadius: Float = 14f
}