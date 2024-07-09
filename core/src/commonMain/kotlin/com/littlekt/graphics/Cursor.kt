package com.littlekt.graphics

import com.littlekt.Disposable

/**
 * @author Colton Daily
 * @date 1/11/2022
 */
expect class Cursor(pixmap: Pixmap, xHotspot: Int = 0, yHotSpot: Int = 0) : Disposable {
    val pixmap: Pixmap
    val xHotspot: Int
    val yHotSpot: Int

    override fun dispose()
}