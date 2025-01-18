package net.mattemade.bossrush.objects

import com.littlekt.Context
import com.littlekt.graphics.MutableColor
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.math.MutableVec2f
import com.littlekt.math.clamp
import net.mattemade.bossrush.shader.ParticleFragmentShader
import net.mattemade.bossrush.shader.ParticleVertexShader
import net.mattemade.bossrush.shader.Particler
import net.mattemade.utils.math.fill
import kotlin.collections.indices
import kotlin.time.Duration

class TextureParticles(
    val context: Context,
    val particleShader: ShaderProgram<ParticleVertexShader, ParticleFragmentShader>,
    val slice: TextureSlice,
    override val position: MutableVec2f,
    val activeFrom: (x: Int, y: Int) -> Float,
    val activeFor: (x: Int, y: Int) -> Float,
    val timeToLive: Float,
    val doubles: Int = 1,
    val interpolation: Int = 1,
    //val doublePosition: Vec2f = position.toMutableVec2().scale(2f).toVec2(),
    val setStartColor: MutableColor.() -> Unit = {},
    val setEndColor: MutableColor.() -> Unit = {},
    val setStartPosition: FloatArray.(x: Int, y: Int) -> Unit = { x, y ->
        fill(
            /*doublePosition.x + */x * 2f,
            /*doublePosition.y + */y * 2f,
        )
    },
    val setEndPosition: FloatArray.(x: Int, y: Int) -> Unit,
): TemporaryDepthRenderableObject {

    val width = slice.width
    val height = slice.height
    val textureData = slice.texture.textureData
    val liveFactor: Float
        get() = particler.time / timeToLive
    private val tempColor = MutableColor()

    private fun FloatArray.fill(vararg value: Float) {
        for (i in indices) {
            this[i] = value[i % value.size]
        }
    }

    private var completed: Boolean = false
    private val particler = Particler(
        context,
        particleShader,
        position,
        width * height * doubles,
        timeToLive,
        2f,
        interpolation = interpolation,
        fillData = { index, startColor, endColor, startPosition, endPosition, activeBetween ->
            val x = (index / doubles) % width
            val y = (index / doubles) / width
            val pixelColor = textureData.pixmap.get(slice.x + x, slice.y + y)
            if (pixelColor == 0) {
                startColor.fill(0f)
                endColor.fill(0f)
                startPosition.fill(0f)
                endPosition.fill(0f)
                activeBetween.fill(0f)
            } else {
                tempColor.setRgba8888(pixelColor)
                setStartColor(tempColor)
                startColor.fill(tempColor.r, tempColor.g, tempColor.b, tempColor.a)
                setEndColor(tempColor)
                endColor.fill(tempColor.r, tempColor.g, tempColor.b, tempColor.a)
                startPosition.setStartPosition(x, y)
                endPosition.setEndPosition(x, y)
                endPosition[0] *= 2f
                endPosition[1] *= 2f
                activeBetween[0] = activeFrom(x, y)
                activeBetween[1] = activeBetween[0] + activeFor(x, y)
            }
        },
        die = {
            completed = true
        }
    )

    fun addToTime(value: Float) {
        particler.time = (particler.time + value).clamp(0f, timeToLive)
    }

    override fun update(dt: Duration): Boolean {
        particler.update(dt)
        return !completed
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        particler.render(batch)
    }

}