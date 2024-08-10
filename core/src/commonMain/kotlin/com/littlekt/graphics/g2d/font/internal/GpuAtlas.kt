package com.littlekt.graphics.g2d.font.internal

import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.Texture
import com.littlekt.graphics.gl.PixmapTextureData

/**
 * @author Colton Daily
 * @date 1/5/2022
 */
internal class GpuAtlas {
    var pixmap = Pixmap(0, 0)
        set(value) {
            field = value
            textureData = PixmapTextureData(field, false)
        }
    var textureData = PixmapTextureData(pixmap, false)
        set(value) {
            field = value
            texture.release()
            texture = Texture(field)
        }
    var texture = Texture(textureData)
    var gridX = 0
    var gridY = 0
    var full = false
    var uploaded = false

    var glyphDataBufOffset = 0

    override fun toString(): String {
        return "GpuAtlas(x=$gridX, y=$gridY, full=$full, uploaded=$uploaded, glyphDataBufOffset=$glyphDataBufOffset)"
    }
}