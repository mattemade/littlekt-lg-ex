package com.littlekt.graphics.gl

import com.littlekt.Context
import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.TextureData

/**
 * @author Colton Daily
 * @date 11/18/2021
 */
class PixmapTextureData(override val pixmap: Pixmap, override val useMipMaps: Boolean) : TextureData {
    override val width: Int
        get() = pixmap.width
    override val height: Int
        get() = pixmap.height
    override var format: Pixmap.Format =
        if (pixmap.glFormat == TextureFormat.RGB) Pixmap.Format.RGB8888 else Pixmap.Format.RGBA8888
    override val isPrepared: Boolean = true
    override val isCustom: Boolean = false

    override fun prepare() {
        throw RuntimeException("prepare() must not be called on a PixmapTextureData instance as it is already prepared.")
    }

    override fun consumePixmap(): Pixmap {
        return pixmap
    }

    override fun consumeCustomData(context: Context, target: TextureTarget) {
        throw RuntimeException("This TextureData implementation does not upload data itself")
    }
}