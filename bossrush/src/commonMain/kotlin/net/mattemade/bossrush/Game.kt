package net.mattemade.bossrush

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.input.InputProcessor
import com.littlekt.input.Key
import com.littlekt.input.Pointer
import com.littlekt.math.floor
import com.littlekt.util.milliseconds
import net.mattemade.bossrush.input.bindInputs
import net.mattemade.bossrush.scene.Fight
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.DirectRender
import kotlin.time.Duration

class Game(
    context: Context,
    private val onLowPerformance: (Boolean) -> Float,
    initialZoom: Float,
) : ContextListener(context),
    Releasing by Self() {

    private var zoom: Float = initialZoom

    var focused = true // TODO: always keep it true? such a hassle to focus/unfocus
        set(value) {
            field = value
            if (!value) {
                context.audio.suspend()
            } else if (value) {
                context.audio.resume()
            }
        }
    private val assets = Assets(context)
    private var audioReady: Boolean = false
    private var assetsReady: Boolean = false
    private val directRender = DirectRender(context, width = 1920, height = 1080, ::update, ::render)
    private var offsetX = 0f
    private var offsetY = 0f
    private var scale = 0f

    private var fpsCheckTimeout: Float = 5000f
    private var framesRenderedInPeriod: Int = 0

    private val input = context.bindInputs()
    private var fight = Fight(context, input, assets)

    override suspend fun Context.start() {
        input.addInputProcessor(object : InputProcessor {
            override fun keyDown(key: Key): Boolean {
                if (!focused) {
                    focused = true
                }
                if (key == Key.ESCAPE) {
                    releaseCursor()
                } else {
                    captureCursor()
                }
                return false
            }

            override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
                if (!focused) {
                    focused = true
                }
                return false
            }
        })

        onResize { width, height ->
            //focused = false
            fpsCheckTimeout = 5000f
            framesRenderedInPeriod = 0
            // resizing to a higher resolution than was before -> going fullscreen
            if (width > directRender.postViewport.virtualWidth || height > directRender.postViewport.virtualHeight) {
                zoom = onLowPerformance(true) // just to reset the zoom factor - it will auto-adjust in 5 seconds after
            }
            directRender.resize(width, height)

            fight.resize(width, height)

            scale = minOf(
                width.toFloat() / VIRTUAL_WIDTH,
                height.toFloat() / VIRTUAL_HEIGHT
            ).floor()
            offsetX = (width - VIRTUAL_WIDTH * scale) / 2f
            offsetY = (height - VIRTUAL_HEIGHT * scale) / 2f
        }

        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            gl.clearColor(Color.CLEAR)

            if (!audioReady) {
                audioReady = audio.isReady()
            }
            if (!assetsReady) {
                assetsReady = audioReady && assets.isLoaded
            }

            if (focused && assetsReady) {

                fight.updateAndRender(dt)
                directRender.render(dt)

                framesRenderedInPeriod++
                fpsCheckTimeout -= dt.milliseconds
                if (fpsCheckTimeout < 0f) {
                    if (framesRenderedInPeriod < 0) { // average is less than 38 fps
                        val canZoomOutEvenMore =
                            directRender.postViewport.virtualWidth > 320 &&
                                    directRender.postViewport.virtualHeight > 180

                        if (canZoomOutEvenMore) {
                            zoom = onLowPerformance(false)
                        }
                    }
                    println("fps: ${framesRenderedInPeriod/5f}")
                    fpsCheckTimeout = 5000f
                    framesRenderedInPeriod = 0
                }
            }
        }

        onDispose(::release)
    }


    private fun update(duration: Duration, camera: Camera) {
        camera.position.set(
            directRender.postViewport.width.toFloat() / 2f,
            directRender.postViewport.height.toFloat() / 2f,
            0f
        )
        camera.update()
    }

    private fun render(duration: Duration, batch: Batch) {
        batch.draw(fight.texture, offsetX, offsetY, width = GAME_WIDTH * scale, height = VIRTUAL_HEIGHT * scale, flipY = true)
        batch.draw(fight.uiTexture, offsetX + GAME_WIDTH * scale, offsetY, width = UI_WIDTH * scale, height = VIRTUAL_HEIGHT * scale, flipY = true)
    }

    companion object {
        const val TITLE = "Boss Rush game"
    }
}
