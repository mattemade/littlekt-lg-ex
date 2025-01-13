package net.mattemade.bossrush.scene

import com.littlekt.Context
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.radians
import com.littlekt.util.fastForEach
import com.littlekt.util.fastIterateRemove
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.UI_WIDTH
import net.mattemade.bossrush.VIRTUAL_HEIGHT
import net.mattemade.bossrush.objects.TemporaryDepthRenderableObject
import net.mattemade.bossrush.objects.TestBoss
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
    private val boss: TestBoss,
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

    private var wasHearts = player.hearts
    private val temporaryObjects = mutableListOf<TemporaryDepthRenderableObject>()

    val halfWidth = assets.texture.heartFilled.width / 2f
    val halfHeight = assets.texture.heartFilled.height / 2f

    private fun renderUi(dt: Duration, batch: Batch) {
        temporaryObjects.fastIterateRemove {
            !it.update(dt)
        }
        (uiShapeRenderer ?: ShapeRenderer(batch).also { uiShapeRenderer = it }).let { shapeRenderer ->

            drawFilledHearts(batch, player.hearts)
            if (wasHearts > player.hearts) {
                for (i in (wasHearts-1) downTo player.hearts) {
                    val startX = 10f + 9f * i
                    val startY = 7f
                    temporaryObjects += TextureParticles(
                        context,
                        particleShader,
                        assets.texture.heartFilled,
                        position = Vec2f(startX, startY),
                        activeFrom = { _, _ -> 0f },
                        activeTo = { _, _ -> 500f },
                        setEndColor = { a = 0f },
                        timeToLive = 500f,
                        interpolation = 3,
                        setEndPosition = { x, y ->
                            fill(
                                startX + halfWidth - (halfWidth - x - 0.5f)*5f,
                                startY + halfHeight - (halfHeight - y - 0.5f)*5f,
                            )
                        }
                        /*setStartPosition = { x, y ->
                            fill(
                                startX + halfWidth - (halfWidth - x - 0.5f)*5f,
                                startY + halfHeight - (halfHeight - y - 0.5f)*5f,
                            )
                        },
                        setEndPosition = {x, y ->
                            fill(
                                startX + x * 2f,
                                startY + y * 2f,
                            )
                        }*/
                    )
                }
            }/* else if (wasHearts < player.hearts) {
                for (i in (wasHearts) until player.hearts) {
                    val startX = 10f + 9f * i
                    val startY = 7f
                    temporaryObjects += TextureParticles(
                        context,
                        particleShader,
                        assets.texture.heartFilled,
                        position = Vec2f(startX, startY),
                        activeFrom = { _, _ -> 0f },
                        activeTo = { _, _ -> 500f },
                        setEndColor = { a = 0f },
                        timeToLive = 500f,
                        interpolation = 3,
                        setEndPosition = { x, y ->
                            fill(
                                startX + halfWidth - (halfWidth - x - 0.5f)*5f,
                                startY + halfHeight - (halfHeight - y - 0.5f)*5f,
                            )
                        }
                    )
                }
                drawFilledHearts(batch, wasHearts)
            } else {

            }*/
            wasHearts = player.hearts

            batch.draw(assets.texture.clockBg, x = 24f, y = 24f, width = 57f, height = 57f)
            val bossNumber = 1
            val totalMinutes = bossNumber * 60 + (1 - boss.health) * 60// absoluteTime// * 20f
            val minutes = totalMinutes % 60f
            val hours = totalMinutes / 60f
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

            temporaryObjects.fastForEach { it.render(batch, shapeRenderer) }
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