package com.littlekt.samples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readAtlas
import com.littlekt.file.vfs.readBitmapFont
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.AnimatedSprite
import com.littlekt.graphics.g2d.TextureArraySpriteBatch
import com.littlekt.graphics.g2d.getAnimation
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.input.Key
import com.littlekt.util.viewport.ScreenViewport

/**
 * @author Colton Daily
 * @date 2/9/2022
 */
class TextureArraySpriteBatchTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val font = resourcesVfs["m5x7_16.fnt"].readBitmapFont()
        val atlas = resourcesVfs["tiles.atlas.json"].readAtlas()
        val person = resourcesVfs["person.png"].readTexture()
        val bossAttackAnim = atlas.getAnimation("bossAttack")
        val boss = AnimatedSprite(bossAttackAnim.firstFrame).apply {
            x = 450f
            y = 250f
            scaleX = 2f
            scaleY = 2f
            playLooped(bossAttackAnim)
        }

        val viewport = ScreenViewport(graphics.width, graphics.height)
        val camera = viewport.camera
        val batch = TextureArraySpriteBatch(this, maxTextureSlots = 3, maxTextureWidth = 256, maxTextureHeight = 1024)

        onResize { width, height ->
            viewport.update(width, height, context)
        }
        onRender { dt ->
            gl.clearColor(Color.DARK_GRAY)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            viewport.apply(context, true)
            boss.update(dt)
            batch.use(camera.viewProjection) {
                boss.render(it)
                it.draw(person, 50f, 200f, scaleX = 5f, scaleY = 5f)
                font.draw(it, "test! --- TEST!!!", 50f, 50f, color = Color.WHITE)
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