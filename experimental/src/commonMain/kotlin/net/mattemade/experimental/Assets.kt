package net.mattemade.experimental

import co.touchlab.stately.collections.ConcurrentMutableList
import co.touchlab.stately.collections.ConcurrentMutableMap
import com.littlekt.Context
import com.littlekt.audio.AudioClipEx
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Texture
import com.littlekt.graphics.gl.TexMagFilter
import com.littlekt.graphics.gl.TexMinFilter
import net.mattemade.utils.asset.AssetPack
import kotlin.math.min

class Assets(context: Context): AssetPack(context) {

    val scenes = ConcurrentMutableList<Texture>()

    private val prepareScenes = listOf(
        "scene0.jpeg",
        "scene1.png",
        "scene2.jpg",
        "scene3.jpg",
    ).forEach { textureName ->
        prepare {
            context.vfs[textureName].readTexture(minFilter = TexMinFilter.LINEAR, magFilter = TexMagFilter.LINEAR).also {
                scenes.add(it)
            }
        }
    }

}