package net.mattemade.bossrush.objects

import com.littlekt.Context
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
import net.mattemade.bossrush.shader.ParticleShader
import net.mattemade.utils.math.fill
import kotlin.random.Random
import kotlin.time.Duration

class SpikeBall(
    private val context: Context,
    private val shader: ParticleShader,
    private val centerPosition: MutableVec2f,
    private val movingRadius: Float = 50f,
    private val angularSpeed: Float = 1f,
    private val assets: Assets,
    private val arena: Arena,
    private val scale: Float = 1f,
) : TemporaryDepthRenderableObject {

    override val position =  MutableVec2f()
    private var rotation: Float = 0f
    private val texture = assets.texture.spikeBall
    private val shadowRadii = MutableVec2f(20f * scale, 10f * scale)

    private val width = texture.width * scale
    private val height = texture.height * scale
    private val halfWidth = width / 2f
    private val halfHeight = height / 2f
    private var appearing = true
    private var disappearing = false
    private var deactivated = false

    override val solidHeight: Float = height

    private val appear = TextureParticles(
        context,
        shader,
        texture,
        position,
        interpolation = 2,
        activeFrom = { x, y -> Random.nextFloat()*100f + (height - y)*20f },
        activeFor = { x, y -> 1000f},
        timeToLive = 1500f,
        setStartColor = { a = 0f },
        setEndColor = { a = 1f },
        setStartPosition = { x, y ->
            fill(-width*2f + width * 4f * Random.nextFloat(), y - height*4f)
        },
        setEndPosition = {x, y ->
            fill(x * scale - halfWidth, y * scale - 50f*scale) // normal rendering offsets
        },
    )

    override fun update(dt: Duration): Boolean {
        if (disappearing) {
            position.set(movingRadius, 0f).rotate((rotation/* + arena.rotation*/).radians).add(centerPosition)
            appear.update(-dt)
            shadowRadii.set(20f, 10f).scale(scale).scale(appear.liveFactor)
            if (appear.liveFactor <= 0f) {
                return false
            }
        } else if (appearing) {
            position.set(movingRadius, 0f).rotate((rotation/* + arena.rotation*/).radians).add(centerPosition)
            appearing = appear.update(dt)
            shadowRadii.set(20f, 10f).scale(scale)
            if (appearing) {
                shadowRadii.scale(appear.liveFactor)
            }
        } else if (!deactivated) {
            rotation += angularSpeed * dt.seconds
            position.set(movingRadius, 0f).rotate((rotation/* + arena.rotation*/).radians).add(centerPosition)
        }
        return true
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        if (appearing || disappearing) {
            appear.render(batch, shapeRenderer)
        } else {
            batch.draw(
                texture,
                x = position.x - halfWidth,
                y = position.y - 50f * scale,
                width = 48f * scale,
                height = 48f * scale,
            )
        }
    }


    override fun isActive(): Boolean = !appearing && !disappearing && !deactivated

    override fun startDisappearing() {
        disappearing = true
    }

    override fun deactivate() {
        deactivated = true
    }

    override fun displace(displacement: Vec2f) {
        // has no effect since position is re-calculated each frame
        //position.add(displacement)
    }


    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledEllipse(
            position,
            shadowRadii,
            innerColor = Color.BLUE.toFloatBits(),
            outerColor = Color.BLACK.toFloatBits()
        )
    }

    override val solidRadius: Float = 20f*scale
}