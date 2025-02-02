package net.mattemade.bossrush

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.graphics.toFloatBits
import com.littlekt.input.InputMapController
import com.littlekt.input.InputProcessor
import com.littlekt.input.Key
import com.littlekt.input.Pointer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.PI_F
import com.littlekt.math.Vec2f
import com.littlekt.math.clamp
import com.littlekt.math.floor
import com.littlekt.math.smoothStep
import com.littlekt.util.fastForEach
import com.littlekt.util.fastForEachWithIndex
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
import kotlin.math.abs
import kotlin.math.pow
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
    var showingIntro: Boolean = !DEBUG
    var showingGame: Boolean = DEBUG
    var showingOutro: Boolean = false
    private var introStaysFor: Float = 0f
    private var introFadesOutIn: Float = 0f
    private var introRotation: Float = -PI2_F
    private var outroRotation: Float = -PI_F
    private val outroTextures by lazy {
        mutableListOf(
            assets.texture.outro1,
            assets.texture.outro2,
            assets.texture.outro3,
            assets.texture.outro4,
        ).mapIndexed { index, texture -> OutroInstance(index, texture, 0f) }
    }
    private val directRender = DirectRender(context, width = 1920, height = 1080, ::update, ::render)
    private val pixelRender = PixelRender(
        context,
        VIRTUAL_WIDTH,
        VIRTUAL_HEIGHT,
        VIRTUAL_WIDTH,
        VIRTUAL_HEIGHT,
        ::updatePixels,
        ::renderPixels,
        clear = true,
    )
    lateinit var particleShader: ShaderProgram<ParticleVertexShader, ParticleFragmentShader>
    lateinit var rotaryShader: RotaryShader
    private var offsetX = 0f
    private var offsetY = 0f
    private var scale = 0f

    private var absoluteGameTime = 0f
    var showMenu: Boolean = false
    private var submenuRotation: Float = 0f
    var showSubmenu: Boolean = false
        set(value) {
            field = value
            submenuRotation = 0f
        }
    private var menuSelection: Int = 0
    private var menuRotation: Float = 0f
    private val menuItems by lazy {
        listOf(
            MenuItem(assets.texture.menuContinue, Vec2f(VIRTUAL_WIDTH/2f, VIRTUAL_HEIGHT*0.75f), { showMenu = false }),
            MenuItem(assets.texture.menuControls, Vec2f(VIRTUAL_WIDTH*0.75f, VIRTUAL_HEIGHT*0.25f), { showSubmenu = true }),
            MenuItem(assets.texture.menuVolume, Vec2f(VIRTUAL_WIDTH*0.25f, VIRTUAL_HEIGHT*0.25f), { showSubmenu = true }),
        )
    }

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

        fight = Fight(context, gameInput, assets, particleShader, { showingOutro = true })

        input.addInputProcessor(object : InputProcessor {
            override fun keyDown(key: Key): Boolean {
                if (!focused) {
                    focused = true
                }
                if (key == Key.ESCAPE) {
                    releaseCursor()
                }
                if (key == Key.ESCAPE || key == Key.BACKSPACE || key == Key.ENTER || key == Key.P) {
                    if (showSubmenu) {
                        showSubmenu = false
                    } else if (showMenu) {
                        showMenu = false
                    } else if (!showingIntro && !showingOutro && showingGame){
                        showMenu = true
                        menuSelection = 0
                        menuRotation = 0f
                    }
                } else {
                    if (!gameInput.gamepadInput) {
                        captureCursor()
                    }
                    if (showMenu) {
                        menuItems[menuSelection].action()
                    }
                }
                return false
            }

            override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
                if (!focused) {
                    focused = true
                }

                captureCursor()
                if (pointer == Pointer.MOUSE_LEFT) {
                    if (showSubmenu) {
                        when (menuSelection) {
                            1 -> { //controls
                                SHOULD_USE_SWING_MODIFIER = !SHOULD_USE_SWING_MODIFIER
                            }
                            2 -> { // volume
                                showSubmenu = false
                            }
                        }
                    } else if (showMenu) {
                        menuItems[menuSelection].action()
                    }
                } else if (showSubmenu) {
                    showSubmenu = false
                } else {
                    showMenu = false
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

                updateSoundTime(dt)
                if (showingGame && !showingOutro && !showMenu) {
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
                    //println("fps: ${framesRenderedInPeriod / 5f}")
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

    private var shapeRenderer: ShapeRenderer? = null
    private val menuBackgroundTint = Color.BLACK.toMutableColor()
    private val indicatorColor = Color(1f, 1f, 1f, 0.75f).toFloatBits()
    private val tempVec2f = MutableVec2f()

    private fun renderPixels(dt: Duration, camera: Camera, batch: Batch) {
        val shapeRenderer = shapeRenderer ?: ShapeRenderer(batch, assets.texture.whitePixel).also { shapeRenderer = it }
        val previousShader = batch.shader
        if (!showingOutro) {
            if (showMenu) {
                /*menuRotation = (menuRotation + gameInput.dRotation) % (menuItems.size * PI2_F)
                batch.shader = rotaryShader // automatically binds the shader
                rotaryShader.fragmentShader.uHeightToWidthRatio.apply(
                    rotaryShader,
                    VIRTUAL_HEIGHT / VIRTUAL_WIDTH.toFloat()
                )
                rotaryShader.fragmentShader.uTime.apply(rotaryShader, menuRotation)
                rotaryShader.fragmentShader.uAlpha.apply(rotaryShader, 1f)
                rotaryShader.fragmentShader.uScale.apply(rotaryShader, 1f - PI_F / abs(menuRotation))*/
            }
            batch.draw(fight.texture, 0f, 0f, width = GAME_WIDTH.toFloat(), height = VIRTUAL_HEIGHT.toFloat())
            batch.draw(
                fight.uiTexture,
                0f,//GAME_WIDTH.toFloat(),
                0f,
                width = UI_WIDTH.toFloat(),
                height = VIRTUAL_HEIGHT.toFloat()
            )
            if (showMenu) {
                //batch.shader = previousShader
            }
        }

        if (showMenu) {
            if (!showSubmenu) {
                menuRotation = (menuRotation + gameInput.dRotation) % (menuItems.size * PI2_F)
            }
            menuBackgroundTint.a = 0.75f//maxOf(0f, abs(menuRotation / PI_F))
            shapeRenderer.filledRectangle(
                x = 0f,
                y = 0f,
                width = VIRTUAL_WIDTH.toFloat(),
                height = VIRTUAL_HEIGHT.toFloat(),
                color = menuBackgroundTint.toFloatBits()
            )
            batch.shader = rotaryShader // automatically binds the shader
            menuItems.fastForEachWithIndex { index, it ->
                if (!showSubmenu || index == menuSelection) {
                    rotaryShader.fragmentShader.uHeightToWidthRatio.apply(
                        rotaryShader,
                        it.texture.height / it.texture.width.toFloat()
                    )
                    for (i in 0..2) { // repeat multiple times for overlap over 0 rad
                        val currentRotation = if (showSubmenu && index == menuSelection) 0f else menuRotation - index * PI2_F + (i - 1f) * PI2_F * menuItems.size
                        val currentRotationFactor = (abs(currentRotation) / PI2_F).clamp(0f, 1.1f)
                        if (currentRotationFactor < 0.5f) {
                            menuSelection = index
                        }
                        rotaryShader.fragmentShader.uTime.apply(rotaryShader, currentRotation)
                        rotaryShader.fragmentShader.uAlpha.apply(rotaryShader, 1f - currentRotationFactor / 1.1f)
                        rotaryShader.fragmentShader.uScale.apply(rotaryShader, 0.5f)
                        batch.draw(
                            it.texture,
                            it.position.x - it.texture.width / 2f,
                            it.position.y - it.texture.height / 2f,
                            width = it.texture.width.toFloat(),
                            height = it.texture.height.toFloat(),
                            flipY = true
                        )
                        batch.flush()
                    }
                }
            }
            batch.shader = previousShader

            if (showSubmenu) {
                when (menuSelection) {
                    1 -> { // controls
                        MOUSE_SENS = (MOUSE_SENS + gameInput.dRotation / 10f).clamp(0.2f, 5f)
                        batch.draw(
                            if (SHOULD_USE_SWING_MODIFIER) assets.texture.submenuControlsManual else assets.texture.submenuControlsAutomatic,
                            0f,
                            0f,
                            width = 240f,
                            height = 240f,
                            flipY = true
                        )
                        batch.draw(
                           assets.texture.submenuControlsSens,
                            275f,
                            VIRTUAL_HEIGHT - 26f,
                            flipY = true
                        )
                        shapeRenderer.circle(335f, 177f, 20f, thickness = 1f, color = Color.WHITE.toFloatBits())
                        shapeRenderer.filledCircle(x = 335f, 177f, 20f * MOUSE_SENS, color = indicatorColor)
                    }
                    2 -> { // volume
                        SOUND_VOLUME = (SOUND_VOLUME + gameInput.dRotation).clamp(0f, 40f)
                        shapeRenderer.circle(VIRTUAL_WIDTH/2f, VIRTUAL_HEIGHT/2f, 40f, thickness = 1f, color = Color.WHITE.toFloatBits())
                        shapeRenderer.filledCircle(x = VIRTUAL_WIDTH/2f, VIRTUAL_HEIGHT/2f, 2f * SOUND_VOLUME, color = indicatorColor)
                        // TODO: update playing music
                    }
                }
            }
        }

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
            batch.shader = rotaryShader // automatically binds the shader
            rotaryShader.fragmentShader.uHeightToWidthRatio.apply(
                rotaryShader,
                assets.texture.xviii.height / assets.texture.xviii.width.toFloat()
            )
            if (introFadesOutIn > 0f) {
                rotaryShader.fragmentShader.uTime.apply(
                    rotaryShader,
                    1f + (3f - introFadesOutIn / 2f) * (6f - introFadesOutIn)
                )
                rotaryShader.fragmentShader.uAlpha.apply(rotaryShader, smoothStep(0f, 6f, introFadesOutIn))
                rotaryShader.fragmentShader.uScale.apply(
                    rotaryShader,
                    0.5f + smoothStep(0f, 0.75f, 0.75f - introFadesOutIn / 8f)
                )
            } else {
                introRotation = (introRotation + gameInput.dRotation).clamp(-PI2_F * 2f, 0f)
                val rotation = if (introStaysFor > 0f) 1f - introStaysFor / 2f else introRotation
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
        if (showingOutro) {
            batch.shader = rotaryShader // automatically binds the shader
            outroRotation = maxOf(-PI2_F * 2f, outroRotation + gameInput.dRotation) + dt.seconds

            val idealAngle = PI2_F * 1.66f * (5) + PI2_F
            val angleDifference = outroRotation - idealAngle
            val alpha = 1f - abs(angleDifference / (PI2_F * 6f))
            rotaryShader.fragmentShader.uTime.apply(rotaryShader, angleDifference)
            rotaryShader.fragmentShader.uAlpha.apply(rotaryShader, alpha)
            rotaryShader.fragmentShader.uScale.apply(rotaryShader, 0.5f)
            rotaryShader.fragmentShader.uHeightToWidthRatio.apply(
                rotaryShader,
                assets.texture.xviii.height / assets.texture.xviii.width.toFloat()
            )
            batch.draw(
                assets.texture.xviii,
                0f,
                0f,
                width = VIRTUAL_WIDTH.toFloat(),
                height = VIRTUAL_HEIGHT.toFloat(),
                flipY = true
            )
            batch.flush()

            outroTextures.fastForEach {
                it.sortFactor = outroRotation - PI2_F * 1.66f * (it.order + 1) - PI_F
            }
            //outroTextures.sortedBy { abs(it.sortFactor) }

            for (i in 0..3) {
                val outroInstance = outroTextures[i]
                val angleDifference = outroInstance.sortFactor
                val alpha = (1f - abs(angleDifference / (PI2_F * 6f))).clamp().pow(2)
                rotaryShader.fragmentShader.uTime.apply(rotaryShader, angleDifference)
                rotaryShader.fragmentShader.uAlpha.apply(rotaryShader, alpha)
                rotaryShader.fragmentShader.uScale.apply(rotaryShader, 1f)
                rotaryShader.fragmentShader.uHeightToWidthRatio.apply(
                    rotaryShader,
                    outroInstance.texture.height / outroInstance.texture.width.toFloat()
                )
                batch.draw(
                    outroInstance.texture,
                    0f,
                    0f,
                    width = VIRTUAL_WIDTH.toFloat(),
                    height = VIRTUAL_HEIGHT.toFloat(),
                    flipY = true
                )
                batch.flush()
            }
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
        const val TITLE = "XVIII"
    }
}

private class MenuItem(val texture: Texture, val position: Vec2f, val action: () -> Unit)

private class OutroInstance(val order: Int, val texture: Texture, var sortFactor: Float)

private fun Float.smoothStep(): Float = com.littlekt.math.smoothStep(0f, 1f, this)
