package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.OrthographicCamera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.font.Text
import com.lehaine.littlekt.graphics.font.TextBlock
import com.lehaine.littlekt.graphics.font.VectorFont
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.toString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * @author Colton Daily
 * @date 12/1/2021
 */
class FontLoadTest(context: Context) : ContextListener(context) {
    lateinit var vectorFont: VectorFont
    private var loading = true
    private val batch = SpriteBatch(this)
    private val camera = OrthographicCamera().apply {
        left = 0f
        right = graphics.width.toFloat()
        bottom = 0f
        top = graphics.height.toFloat()
    }

    init {
        Logger.defaultLevel = Logger.Level.DEBUG
        logger.level = Logger.Level.DEBUG
        fileHandler.launch {
            val font = loadTtfFont("FreeSerif.ttf")
            vectorFont = VectorFont(font)
            loading = false
        }
    }

    var text = "Hello"
    var text2 = "Hwllo"
    var usingText = true
    var step = 1
    var totalTime: Duration = 0.seconds
    var startValue = 72

    private var lastStats = "Loading..."
    val staticText = Text("FPS: ", 36, Color.GREEN)
    val fpsText = Text("TBD", 36, Color.RED)
    val textBlock =
        TextBlock(50f, 430f, mutableListOf(staticText, fpsText, Text("\nHELLO!!!!!!!!!!!!!!!!!", pxScale = 36)))
    val textBlock2 = TextBlock(
        50f,
        230f,
        mutableListOf(Text("My name is colt and i am cool yeah test water and coffee and food and yum and wwwwwwwwww\nwwwwwwwwwwwwwwwwwwww\n testeststst\n and i am just writing nonsense to fill up the screen. this is what you do when you see it all. LOL!!\nhahdaufhasudfhu aosufoasdfasdfhuoa ssdfon d", pxScale = 36))
    )

    override fun render(dt: Duration) {
        if (loading) return
        if (!loading && !vectorFont.prepared) {
            vectorFont.prepare(this)
        }
        camera.update()
        gl.clearColor(Color.DARK_GRAY)
        gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

        fpsText.text = stats.fps.toString(1)
        vectorFont.queue(textBlock2)
        vectorFont.queue(textBlock)
        vectorFont.flush(batch, camera.viewProjection, useJitter = false)
//
//        gpuFont.fontSize = 100
//        gpuFont.buildText(50f, 330f) {
//            append(Color.WHITE) { "This" }
//            append(Color.GREEN) { " is" }
//            append(Color.BLUE) { " awesome!" }
//            append(Color.RED) { "!" }
//            append(Color.YELLOW) { "!" }
//        }
//        gpuFont.flush(camera.viewProjection)
//
//        totalTime += dt
//        if (totalTime >= 10.milliseconds) {
//            startValue += step
//            totalTime = 0.seconds
//            if (startValue >= 400) {
//                step = -step
//            }
//            if (startValue <= 1) {
//                step = -step
//            }
//        }
//        gpuFont.fontSize = startValue
//        gpuFont.text("Offscreen, with jitter", 50f, 230f)
//        gpuFont.flush(batch, camera.viewProjection, Color.GREEN)
//
//        gpuFont.fontSize = 48
//        gpuFont.text("Offscreen, no jitter", 50f, 130f)
//        gpuFont.flush(batch, camera.viewProjection, useJitter = false)


        lastStats = stats.toString()
        if (input.isKeyJustPressed(Key.ENTER)) {
            usingText = !usingText
        }

        if (input.isKeyJustPressed(Key.P)) {
            logger.debug { stats.toString() }
        }

        if (input.isKeyJustPressed(Key.ESCAPE)) {
            close()
        }
    }

    override fun resize(width: Int, height: Int) {
        if (loading) return
        camera.right = width.toFloat()
        camera.top = height.toFloat()
        vectorFont.resize(width, height, this)
    }
}