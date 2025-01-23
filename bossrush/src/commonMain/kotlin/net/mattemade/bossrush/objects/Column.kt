package net.mattemade.bossrush.objects

import com.littlekt.Context
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.shader.ParticleShader
import net.mattemade.utils.math.fill
import kotlin.random.Random
import kotlin.time.Duration

class Column(override val position: MutableVec2f, private val assets: Assets, context: Context, shader: ParticleShader): TemporaryDepthRenderableObject {

    private val texture = assets.texture.column
    private val width = texture.width
    private val widthFloat = texture.width.toFloat()
    private val height = texture.height
    private val heightFloat = height.toFloat()
    private val halfWidth = width / 2f
    private val halfHeight = height / 2f
    private var appearing = true
    private var disappearing = false

    override val solidHeight: Float = 50f
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
            fill(x - halfWidth, y - 56f) // normal rendering offsets
        },
    )

    override fun displace(displacement: Vec2f) {
        position.add(displacement)
    }

    override val solidRadius: Float
        get() = 16f

    override fun update(dt: Duration): Boolean {
        if (disappearing) {
            appear.update(-dt)
            disappearing = appear.liveFactor > 0f
            if (!disappearing) {
                return false
            }
        } else if (appearing) {
            appearing = appear.update(dt)
        }
        return true
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        if (appearing || disappearing) {
            appear.render(batch, shapeRenderer)
        } else {
            batch.draw(assets.texture.column, x = position.x - halfWidth, y = position.y - 56f, width = widthFloat, height = heightFloat)
        }
    }

    override fun isActive(): Boolean = !disappearing

    override fun startDisappearing() {
        disappearing = true
    }
}