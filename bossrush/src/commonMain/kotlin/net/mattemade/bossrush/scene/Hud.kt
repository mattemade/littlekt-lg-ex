package net.mattemade.bossrush.scene

import com.littlekt.Context
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.floorToInt
import com.littlekt.math.geom.radians
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

    private val fadeOutColor = Color.BLACK.toMutableColor().apply { a = 0.5f }.toFloatBits()

    private val heartTextures = Array(20) { index ->
        val startX = 10f + (heartSlices[0].width + 1) * (index % 10)
        val startY = 7f + (heartSlices[0].height + 1) * (index / 10)
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

    private fun renderUi(dt: Duration, batch: Batch) {
        (uiShapeRenderer ?: ShapeRenderer(batch, slice = assets.texture.whitePixel).also { uiShapeRenderer = it }).let { shapeRenderer ->

            for (i in 0 until player.maxHearts) {
                batch.draw(
                    heartSlices[1],
                    x = 10f + (heartSlices[1].width + 1) * (i % 10),
                    y = 7f + (heartSlices[1].height + 1) * (i / 10),
                    width = heartSlices[1].width.toFloat(),
                    height = heartSlices[1].height.toFloat()
                )

                val heartTexture = heartTextures[i]
                heartTexture.addToTime(if (player.hearts > i) -dt.milliseconds else dt.milliseconds)
                heartTexture.render(batch, shapeRenderer)
            }


            val offset = VIRTUAL_WIDTH - 100
            batch.draw(assets.texture.mock, x = offset.toFloat(), y = 90f, width = 100f, height = 150f)
            for (i in 1..player.resources) {
                val row = (i - 1) % 6
                val column = (i - 1) / 6
                batch.draw(
                    assets.texture.resource,
                    x = offset + 10f + 10f * column,
                    y = 150f - 10f * row,
                    width = 9f,
                    height = 9f
                )
            }
            val selectorPosition = minOf(player.placingTrapForSeconds, 3f)
            if (selectorPosition > 0f) {
                batch.draw(
                    assets.texture.selector,
                    x = offset + 38f,
                    y = 155f - selectorPosition * 19f,
                    width = 10f,
                    height = 5f
                )

                val trap = minOf(3, selectorPosition.floorToInt() + 1)
                for (i in 1..3) {
                    if (i != trap) {
                        shapeRenderer.filledRectangle(
                            x = offset + 50f,
                            y = 155f - i * 19f,
                            width = 50f,
                            height = 22f,
                            color = fadeOutColor
                        )
                    }
                }
            } else {

            }
            val trapAvailable =
                if (player.resources >= 10) 3 else if (player.resources >= 5) 2 else if (player.resources >= 2) 1 else 0
            shapeRenderer.filledRectangle(
                x = offset + 50f,
                y = 155f - 3 * 19f,
                width = 50f,
                height = 22f + 2 * 19f - trapAvailable*19f,
                color = fadeOutColor
            )

        }
    }

    private fun drawFilledHearts(batch: Batch, limit: Int) {
        for (i in 0..4) {
            batch.draw(
                if (i < limit) assets.texture.heartFilled else assets.texture.heartEmpty,
                x = 10f + 9f * i,
                y = 7f,
                width = 8f,
                height = 6f
            )
        }
    }
}