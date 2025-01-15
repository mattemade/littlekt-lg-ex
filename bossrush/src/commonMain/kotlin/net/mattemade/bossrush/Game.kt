package net.mattemade.bossrush

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.input.InputMapController
import com.littlekt.input.InputProcessor
import com.littlekt.input.Key
import com.littlekt.input.Pointer
import com.littlekt.math.PI2_F
import com.littlekt.math.PI_F
import com.littlekt.math.clamp
import com.littlekt.math.floor
import com.littlekt.math.smoothStep
import com.littlekt.util.milliseconds
import com.littlekt.util.seconds
import net.mattemade.bossrush.input.ControllerInput
import net.mattemade.bossrush.input.GameInput
import net.mattemade.bossrush.input.bindInputs
import net.mattemade.bossrush.scene.Fight
import net.mattemade.bossrush.shader.ParticleFragmentShader
import net.mattemade.bossrush.shader.ParticleVertexShader
import net.mattemade.bossrush.shader.RotaryShader
import net.mattemade.bossrush.shader.createParticleShader
import net.mattemade.bossrush.shader.createRotaryShader
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.DirectRender
import net.mattemade.utils.render.PixelRender
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
    private var showingIntro: Boolean = true
    private var showingGame: Boolean = false
    private var introStaysFor: Float = 0f
    private var introFadesOutIn: Float = 0f
    private var introRotation: Float = -PI2_F
    private val directRender = DirectRender(context, width = 1920, height = 1080, ::update, ::render)
    private val pixelRender = PixelRender(
        context,
        VIRTUAL_WIDTH,
        VIRTUAL_HEIGHT,
        VIRTUAL_WIDTH,
        VIRTUAL_HEIGHT,
        ::updatePixels,
        ::renderPixels,
        clear = true
    )
    lateinit var particleShader: ShaderProgram<ParticleVertexShader, ParticleFragmentShader>
    lateinit var rotaryShader: RotaryShader
    private var offsetX = 0f
    private var offsetY = 0f
    private var scale = 0f

    private var absoluteGameTime = 0f

    private var fpsCheckTimeout: Float = 5000f
    private var framesRenderedInPeriod: Int = 0

    private val inputController: InputMapController<ControllerInput> = context.bindInputs()
    private val gameInput: GameInput = GameInput(context, inputController)
    private lateinit var fight: Fight

    override suspend fun Context.start() {
        particleShader = createParticleShader(
            vfs["shader/particles.vert.glsl"].readString(),
            vfs["shader/particles.frag.glsl"].readString()
        ).also { it.prepare(this) }

        rotaryShader = createRotaryShader(
            vfs["shader/rotary.frag.glsl"].readString()
        ).also { it.prepare(this) }

        fight = Fight(context, gameInput, assets, particleShader)

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

            scale = minOf(
                width.toFloat() / VIRTUAL_WIDTH,
                height.toFloat() / VIRTUAL_HEIGHT
            ).floor()
            offsetX = (width - VIRTUAL_WIDTH * scale) / 2f
            offsetY = (height - VIRTUAL_HEIGHT * scale) / 2f
        }

        onRender { dt ->
            absoluteGameTime += dt.seconds
            gameInput.update(dt)

            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            gl.clearColor(Color.CLEAR)

            if (!audioReady) {
                audioReady = audio.isReady()
            }
            if (!assetsReady) {
                assetsReady = audioReady && assets.isLoaded
            }

            if (focused && assetsReady) {

                if (showingGame) {
                    fight.updateAndRender(dt)
                }
                pixelRender.render(dt)
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
                    println("fps: ${framesRenderedInPeriod / 5f}")
                    fpsCheckTimeout = 5000f
                    framesRenderedInPeriod = 0
                }
            }
        }

        onDispose(::release)
    }


    private fun updatePixels(duration: Duration, camera: Camera) {
        camera.position.set(HALF_VIRTUAL_WIDTH, HALF_VIRTUAL_HEIGHT, 0f)
        camera.update()
    }

    private fun renderPixels(dt: Duration, camera: Camera, batch: Batch) {
        batch.draw(fight.texture, 0f, 0f, width = GAME_WIDTH.toFloat(), height = VIRTUAL_HEIGHT.toFloat())
        batch.draw(
            fight.uiTexture,
            GAME_WIDTH.toFloat(),
            0f,
            width = UI_WIDTH.toFloat(),
            height = VIRTUAL_HEIGHT.toFloat()
        )

        if (introFadesOutIn > 0f) {
            introFadesOutIn -= dt.seconds
            if (introFadesOutIn <= 0f) {
                showingIntro = false
            }
        }
        if (introStaysFor > 0f) {
            introStaysFor -= dt.seconds
            if (introStaysFor <= 0f) {
                introFadesOutIn = 6f
                showingGame = true
            }
        }
        if (showingIntro) {
            val previousShader = batch.shader
            batch.shader = rotaryShader // automatically binds the shader
            if (introFadesOutIn > 0f) {
                rotaryShader.fragmentShader.uTime.apply(rotaryShader, 1f + (3f - introFadesOutIn/2f)*(6f - introFadesOutIn))
                rotaryShader.fragmentShader.uAlpha.apply(rotaryShader, smoothStep(0f, 6f, introFadesOutIn))
                rotaryShader.fragmentShader.uScale.apply(rotaryShader, 0.5f + smoothStep(0f, 0.75f, 0.75f - introFadesOutIn/8f))
            } else {
                introRotation = (introRotation + gameInput.dRotation).clamp(-PI2_F * 2f, 0f)
                val rotation = if (introStaysFor > 0f) 1f - introStaysFor/2f else introRotation
                rotaryShader.fragmentShader.uTime.apply(rotaryShader, rotation)
                rotaryShader.fragmentShader.uAlpha.apply(rotaryShader, 1f)
                rotaryShader.fragmentShader.uScale.apply(rotaryShader, 0.5f)
                if (introStaysFor == 0f && introRotation == 0f) {
                    introStaysFor = 2f
                }
            }
            batch.draw(
                assets.texture.xviii,
                0f,
                0f,
                width = VIRTUAL_WIDTH.toFloat(),
                height = VIRTUAL_HEIGHT.toFloat(),
                flipY = true
            )
            batch.shader = previousShader
        }
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
        batch.draw(
            pixelRender.texture,
            offsetX,
            offsetY,
            width = VIRTUAL_WIDTH * scale,
            height = VIRTUAL_HEIGHT * scale,
            flipY = false
        )
    }

    companion object {
        const val TITLE = "Boss Rush game"
    }
}

private fun Float.smoothStep(): Float = com.littlekt.math.smoothStep(0f, 1f, this)
