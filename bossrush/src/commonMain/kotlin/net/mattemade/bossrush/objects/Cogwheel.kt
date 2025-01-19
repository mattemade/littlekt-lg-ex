package net.mattemade.bossrush.objects

import com.littlekt.Context
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.Vec2f
import com.littlekt.math.floorToInt
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.input.GameInput
import net.mattemade.bossrush.player.Player
import net.mattemade.bossrush.shader.ParticleShader
import net.mattemade.utils.math.fill
import kotlin.random.Random
import kotlin.time.Duration

class Cogwheel(
    override val position: MutableVec2f,
    private val player: Player,
    private val input: GameInput,
    private val assets: Assets,
    private val context: Context,
    private val shader: ParticleShader,
) : TemporaryDepthRenderableObject {

    var rotation = 0f
    private val textures = listOf(assets.texture.cogwheelTurn0, assets.texture.cogwheelTurn1, assets.texture.cogwheelTurn2)
    private val segments = listOf(0, 1, 2, 0, 1, 2).map { textures[it] }
    private val positions = segments.size
    private val radInSegment = PI2_F / positions



    private val texture = segments[0]
    private val width = texture.width
    private val height = texture.height
    private val heightFloat = height.toFloat()
    private val halfWidth = width / 2f
    private var appearing = true
    private var disappearing = false

    private val appear = TextureParticles(
        context,
        shader,
        texture,
        position,
        interpolation = 2,
        activeFrom = { x, y -> Random.nextFloat()*300f + (height - y)*30f },
        activeFor = { x, y -> 2000f},
        timeToLive = 4000f,
        setStartColor = { a = 0f },
        setEndColor = { a = 1f },
        setStartPosition = { x, y ->
            fill(-width*2f + width * 4f * Random.nextFloat(), y - heightFloat*4f)
        },
        setEndPosition = {x, y ->
            fill(x - halfWidth, y - 40f) // normal rendering offsets
        },
    )

    override fun update(dt: Duration): Boolean {
        if (disappearing) {
            appear.update(-dt)
            if (appear.liveFactor <= 0f) {
                return false
            }
        } else if (appearing) {
            appearing = appear.update(dt)
        } else if (player.position.distance(position) < player.solidRadius * 5f + solidRadius) {
            rotation -= input.dRotation
        }
        return true
    }

    override fun startDisappearing() {
        disappearing = true
    }

    override fun isActive(): Boolean = !appearing && !disappearing

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        if (appearing || disappearing) {
            appear.render(batch, shapeRenderer)
        } else {
            val segment = ((rotation / radInSegment).floorToInt() % positions + positions) % positions
            batch.draw(
                segments[segment],
                x = position.x - 16f,
                y = position.y - 40f,
                width = 32f,
                height = 48f,
            )
        }
    }

    override fun displace(displacement: Vec2f) {
        position.add(displacement)
    }

    override val solidRadius: Float = 14f
}