package com.littlekt.samples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.Experimental
import com.littlekt.file.vfs.readTtfFont
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.font.VectorFont
import com.littlekt.graphics.g2d.font.VectorFont.*
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.input.Key
import com.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 10/21/2022
 */
class VectorFontTest(context: Context) : ContextListener(context) {

    @OptIn(Experimental::class)
    override suspend fun Context.start() {
        val viewport = ExtendViewport(480, 270)
        val camera = viewport.camera
        val freeSerif = resourcesVfs["FreeSerif.ttf"].readTtfFont()
        val vectorFont = VectorFont(freeSerif).also { it.prepare(this) }
        val text = TextBlock(5f, 5f, mutableListOf(Text("You're not real man", 16, Color.RED)))
        val text2 = TextBlock(50f, 200f, mutableListOf(Text("Had a funeral for a bird", 32, Color.RED)))
        var lastStats = ""

        onResize { width, height ->
            viewport.update(width, height, this, true)
            vectorFont.resize(width, height, this)
        }

        onRender { dt ->
            gl.clearColor(Color.DARK_GRAY)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            camera.update()

            text.text[0].text = lastStats
            vectorFont.queue(text)
            vectorFont.queue(text2)
            vectorFont.flush(camera.viewProjection)

            lastStats = stats.toString()
            if (input.isKeyPressed(Key.V)) {
                text.text[0].pxScale++
            }
            if (input.isKeyPressed(Key.C)) {
                text.text[0].pxScale--
            }
            if (input.isKeyPressed(Key.W)) {
                text.y -= 10f
            }
            if (input.isKeyPressed(Key.S)) {
                text.y += 10f
            }
            if (input.isKeyPressed(Key.D)) {
                text.x += 10f
            }
            if (input.isKeyPressed(Key.A)) {
                text.x -= 10f
            }
            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}