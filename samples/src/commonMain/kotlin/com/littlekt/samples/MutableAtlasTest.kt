package com.littlekt.samples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readAtlas
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.g2d.AnimatedSprite
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.getAnimation
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.input.Key
import com.littlekt.util.combine
import com.littlekt.util.viewport.ScreenViewport

/**
 * @author Colton Daily
 * @date 2/8/2022
 */
class MutableAtlasTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val fntTexture = resourcesVfs["m5x7_16_0.png"].readTexture()
        val atlas = resourcesVfs["tiles.atlas.json"].readAtlas().combine(fntTexture, "font", this)
        val bossAttackAnim = atlas.getAnimation("bossAttack")
        val boss = AnimatedSprite(bossAttackAnim.firstFrame).apply {
            x = 450f
            y = 350f
            scaleX = 2f
            scaleY = 2f
            playLooped(bossAttackAnim)
        }

        val viewport = ScreenViewport(graphics.width, graphics.height)
        val camera = viewport.camera
        val batch = SpriteBatch(this)

        onResize { width, height ->
            viewport.update(width, height, context)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            camera.update()
            boss.update(dt)
            batch.use(camera.viewProjection) {
                it.draw(atlas["font"].slice, 450f, 150f, scaleX = 4f, scaleY = 4f)
                boss.render(it)
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