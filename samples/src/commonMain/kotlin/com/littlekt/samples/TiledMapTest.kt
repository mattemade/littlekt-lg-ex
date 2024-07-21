package com.littlekt.samples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readPixmap
import com.littlekt.file.vfs.readTexture
import com.littlekt.file.vfs.readTiledMap
import com.littlekt.graphics.addBorderToSlices
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.tilemap.tiled.TiledMap
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.slice
import com.littlekt.input.Key
import com.littlekt.util.MutableTextureAtlas
import com.littlekt.util.milliseconds
import com.littlekt.util.viewport.ExtendViewport
import kotlin.math.roundToInt

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
class TiledMapTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {

        val viewport = ExtendViewport(30, 16)
        val camera = viewport.camera

        val batch = SpriteBatch(context, 8191)

        // load the textures manually for the ortho map
        val cavernasTexture =
            resourcesVfs["tiled/Cavernas_by_Adam_Saltsman.png"].readPixmap().addBorderToSlices(context, 8, 8, 2)
        val background = resourcesVfs["ldtk/N2D - SpaceWallpaper1280x448.png"].readTexture()
        val atlas = MutableTextureAtlas(this)
            .add(cavernasTexture.slice(), "Cavernas_by_Adam_Saltsman.png")
            .add(background.slice(), "N2D - SpaceWallpaper1280x448.png")
            .toImmutable()

        // we need to dispose of them if we aren't using them since the atlas generates new textures
        cavernasTexture.release()
        background.release()

        val maps = mutableListOf<TiledMap>()
        resourcesVfs["tiled/ortho-tiled-world.tmj"].readTiledMap(atlas).also { maps += it }
        resourcesVfs["tiled/iso-tiled-world.tmj"].readTiledMap().also { maps += it }
        resourcesVfs["tiled/staggered-tiled-world.tmj"].readTiledMap().also { maps += it }

        var mapIdx = 0
        onResize { width, height ->
            viewport.update(width, height, context)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            if (input.isKeyPressed(Key.W)) {
                camera.position.y -= 0.1f * dt.milliseconds
            } else if (input.isKeyPressed(Key.S)) {
                camera.position.y += 0.1f * dt.milliseconds
            }

            if (input.isKeyPressed(Key.D)) {
                camera.position.x += 0.1f * dt.milliseconds
            } else if (input.isKeyPressed(Key.A)) {
                camera.position.x -= 0.1f * dt.milliseconds
            }

            camera.position.x = camera.position.x.roundToInt().toFloat()
            camera.position.y = camera.position.y.roundToInt().toFloat()
            camera.update()

            batch.use(camera.viewProjection) {
                maps[mapIdx].render(it, camera, scale = 1f / 8f, displayObjects = true)
            }

            if (input.isKeyJustPressed(Key.ENTER)) {
                mapIdx++
                if (mapIdx >= maps.size) mapIdx = 0
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