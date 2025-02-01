package net.mattemade.bossrush.scene

import com.littlekt.Context
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.graphics.toFloatBits
import com.littlekt.graphics.util.BlendMode
import com.littlekt.math.MutableVec2f
import com.littlekt.util.milliseconds
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.UI_WIDTH
import net.mattemade.bossrush.VIRTUAL_HEIGHT
import net.mattemade.bossrush.VIRTUAL_WIDTH
import net.mattemade.bossrush.objects.TextureParticles
import net.mattemade.bossrush.player.Player
import net.mattemade.bossrush.shader.ParticleFragmentShader
import net.mattemade.bossrush.shader.ParticleVertexShader
import net.mattemade.utils.math.fill
import net.mattemade.utils.render.PixelRender
import kotlin.time.Duration

class Hud(
    private val context: Context,
    private val particleShader: ShaderProgram<ParticleVertexShader, ParticleFragmentShader>,
    private val assets: Assets,
    private val player: Player,
) {

    private var uiRenderer = PixelRender(
        context,
        targetWidth = UI_WIDTH,
        targetHeight = VIRTUAL_HEIGHT,
        preRenderCall = { dt, camera ->
            camera.position.set(UI_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0f)
            camera.update()
        },
        renderCall = { dt, camera, batch ->
            renderUi(dt, batch)
        },
        worldWidth = UI_WIDTH,
        worldHeight = VIRTUAL_HEIGHT,
        clear = true,
    )
    val uiTexture = uiRenderer.texture

    private var uiShapeRenderer: ShapeRenderer? = null
    private var absoluteTime: Float = 0f
    private val tempVec2f = MutableVec2f()

    fun updateAndRender(dt: Duration) {
        absoluteTime += dt.seconds
        uiRenderer.render(dt)
    }

    private val heartSlices = assets.texture.heart.slice(sliceWidth = assets.texture.heart.width / 2, sliceHeight = assets.texture.heart.height)[0]
    val halfWidth = heartSlices[0].width / 2f
    val halfHeight = heartSlices[0].height / 2f

    private val heartTextures = Array(40) { index ->
        val startX = 10f + (heartSlices[0].width + 1) * (index % 20)
        val startY = 7f + (heartSlices[0].height + 1) * (index / 20)
        TextureParticles(
            context,
            particleShader,
            heartSlices[0],
            position = MutableVec2f(startX, startY),
            activeFrom = { _, _ -> 0f },
            activeFor = { _, _ -> 500f },
            setEndColor = { a = 0f },
            timeToLive = 500f,
            interpolation = 3,
            setEndPosition = { x, y ->
                fill(
                    halfWidth - (halfWidth - x - 0.5f) * 5f,
                    halfHeight - (halfHeight - y - 0.5f) * 5f,
                )
            }
        ).also {
            it.addToTime(500f)
        }
    }

    private val filledBallsColor: Float = Color.WHITE.toMutableColor().apply { a = 0.0f }.toFloatBits()
    private val diselectedItemColor: Float = Color.WHITE.toMutableColor().apply { a = 0.85f }.toFloatBits()
    private val selectedItemColor: Float = Color.WHITE.toFloatBits()

    private fun renderUi(dt: Duration, batch: Batch) {
        (uiShapeRenderer ?: ShapeRenderer(batch, slice = assets.texture.whitePixel).also { uiShapeRenderer = it }).let { shapeRenderer ->

            for (i in 0 until player.maxHearts) {
                batch.draw(
                    heartSlices[1],
                    x = 10f + (heartSlices[1].width + 1) * (i % 20),
                    y = 7f + (heartSlices[1].height + 1) * (i / 20),
                    width = heartSlices[1].width.toFloat(),
                    height = heartSlices[1].height.toFloat()
                )

                val heartTexture = heartTextures[i]
                heartTexture.addToTime(if (player.hearts > i) -dt.milliseconds else dt.milliseconds)
                heartTexture.render(batch, shapeRenderer)
            }

            val offset = VIRTUAL_WIDTH - 10
            for (i in 1..player.resources) {
                batch.draw(
                    assets.texture.resource,
                    x = offset - i*(assets.texture.resource.width + 1).toFloat(),// offset + 10f + 10f * column,
                    y = 5f,
                    width = assets.texture.resource.width.toFloat(),
                    height = assets.texture.resource.height.toFloat()
                )
            }

            batch.setBlendFunction(BlendMode.Alpha)
            val selectorPosition = minOf(player.placingTrapForSeconds, 3f)
            val filledBalls = if (selectorPosition >= 2f) 10 else if (selectorPosition >= 1f) 5 else if (selectorPosition > 0f) 2 else 0
            val filledWidth = 10f + filledBalls * (assets.texture.resource.width + 1)
            shapeRenderer.filledRectangle(
                x = VIRTUAL_WIDTH - filledWidth,
                y = 0f,
                width = filledWidth,
                height = 5f + assets.texture.resource.height + 2f,
                color = filledBallsColor
            )
            batch.setToPreviousBlendFunction()

            var xOffset = 10f
            val yOffset = 7f + assets.texture.resource.height
            if (player.resources >= 2) {
                xOffset += assets.texture.ballIcon.width
                batch.draw(
                    assets.texture.ballIcon,
                    x = VIRTUAL_WIDTH - xOffset,
                    y = yOffset,
                    width = assets.texture.ballIcon.width.toFloat(),
                    height = assets.texture.ballIcon.height.toFloat(),
                    colorBits = if (selectorPosition >= 1f) diselectedItemColor else selectedItemColor,
                )
            }
            if (player.resources >= 5) {
                xOffset += 2f + assets.texture.trapIcon.width
                batch.draw(
                    assets.texture.trapIcon,
                    x = VIRTUAL_WIDTH - xOffset,
                    y = yOffset,
                    width = assets.texture.trapIcon.width.toFloat(),
                    height = assets.texture.trapIcon.height.toFloat(),
                    colorBits = if (selectorPosition == 0f || selectorPosition >= 1f && selectorPosition < 2f) selectedItemColor else diselectedItemColor,
                )
            }
            if (player.resources >= 10) {
                xOffset += 2f + assets.texture.healIcon.width
                batch.draw(
                    assets.texture.healIcon,
                    x = VIRTUAL_WIDTH - xOffset,
                    y = yOffset,
                    width = assets.texture.healIcon.width.toFloat(),
                    height = assets.texture.healIcon.height.toFloat(),
                    colorBits = if (selectorPosition == 0f || selectorPosition >= 2f) selectedItemColor else diselectedItemColor,
                )
            }
        }
    }
}