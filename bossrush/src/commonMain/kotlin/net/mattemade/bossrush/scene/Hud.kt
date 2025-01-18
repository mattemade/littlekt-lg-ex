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
    private val bossHealth: () -> Float,
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
    var currentHour = 0

    fun updateAndRender(dt: Duration) {
        absoluteTime += dt.seconds
        uiRenderer.render(dt)
    }

    val halfWidth = assets.texture.heartFilled.width / 2f
    val halfHeight = assets.texture.heartFilled.height / 2f

    private val fadeOutColor = Color.BLACK.toMutableColor().apply { a = 0.5f }.toFloatBits()

    private val heartTextures = Array(5) { index ->
        val startX = 10f + 9f * index
        val startY = 7f
        TextureParticles(
            context,
            particleShader,
            assets.texture.heartFilled,
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
        )
    }

    private fun renderUi(dt: Duration, batch: Batch) {
        (uiShapeRenderer ?: ShapeRenderer(batch).also { uiShapeRenderer = it }).let { shapeRenderer ->

            for (i in 0..4) {
                batch.draw(
                    assets.texture.heartEmpty,
                    x = 10f + 9f * i,
                    y = 7f,
                    width = 8f,
                    height = 6f
                )

                val heartTexture = heartTextures[i]
                heartTexture.addToTime(if (player.hearts > i) -dt.milliseconds else dt.milliseconds)
                heartTexture.render(batch, shapeRenderer)
            }

            batch.draw(assets.texture.clockBg, x = 24f, y = 24f, width = 57f, height = 57f)
            //val bossNumber = 1
            val totalMinutes = currentHour * 60 + (1 - bossHealth()) * 60// absoluteTime// * 20f
            val minutes = (1 - bossHealth()) * 60f
            val hours = currentHour + minutes / 60f
            val minutesRotation = minutes * PI2_F / 60f// + PI_F/2f
            val hoursRotation = hours * PI2_F / 4f// + PI_F/2f
            tempVec2f.set(-57f / 2f, -57 / 2f).rotate(hoursRotation.radians)
            batch.draw(
                assets.texture.clockHour,
                x = 24f + 57 / 2f + tempVec2f.x,
                y = 24f + 57f / 2f + tempVec2f.y,
                width = 57f,
                height = 57f,
                rotation = hoursRotation.radians
            )
            tempVec2f.set(-57f / 2f, -57 / 2f).rotate(minutesRotation.radians)
            batch.draw(
                assets.texture.clockMinute,
                x = 24f + 57 / 2f + tempVec2f.x,
                y = 24f + 57f / 2f + tempVec2f.y,
                width = 57f,
                height = 57f,
                rotation = minutesRotation.radians
            )


            batch.draw(assets.texture.mock, x = 0f, y = 90f, width = 100f, height = 150f)
            for (i in 1..player.resources) {
                val row = (i - 1) % 6
                val column = (i - 1) / 6
                batch.draw(
                    assets.texture.resource,
                    x = 10f + 10f * column,
                    y = 150f - 10f * row,
                    width = 9f,
                    height = 9f
                )
            }
            val selectorPosition = minOf(player.placingTrapForSeconds, 3f)
            if (selectorPosition > 0f) {
                batch.draw(
                    assets.texture.selector,
                    x = 38f,
                    y = 155f - selectorPosition * 19f,
                    width = 10f,
                    height = 5f
                )

                val trap = minOf(3, selectorPosition.floorToInt() + 1)
                for (i in 1..3) {
                    if (i != trap) {
                        shapeRenderer.filledRectangle(
                            x = 50f,
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
                x = 50f,
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