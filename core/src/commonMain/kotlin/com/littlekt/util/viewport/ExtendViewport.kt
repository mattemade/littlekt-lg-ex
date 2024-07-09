package com.littlekt.util.viewport

import com.littlekt.Context
import com.littlekt.graphics.Camera
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.util.Scaler
import kotlin.math.roundToInt

/**
 * A viewport that supports using a virtual size.
 * The virtual viewport maintains the aspect ratio by extending the game world horizontally or vertically.
 * The world is scaled to fit within the viewport and then the shorter dimension is lengthened to fill the viewport.
 * @author Colton Daily
 * @date 12/27/2021
 */
class ExtendViewport(val minWidth: Int, val minHeight: Int, camera: Camera = OrthographicCamera()) :
    Viewport(0, 0, minWidth, minHeight, camera) {

    override fun update(width: Int, height: Int, context: Context, centerCamera: Boolean) {
        var worldWidth = minWidth.toFloat()
        var worldHeight = minHeight.toFloat()

        val scaled = Scaler.Fit().apply(minWidth, minHeight, width, height)
        var viewportWidth = scaled.x.roundToInt()
        var viewportHeight = scaled.y.roundToInt()
        if (viewportWidth < width) {
            val toViewportSpace = viewportHeight / worldHeight
            val toWorldSpace = worldHeight / viewportHeight
            val lengthen = (width - viewportWidth) * toWorldSpace
            worldWidth += lengthen
            viewportWidth += (lengthen * toViewportSpace).roundToInt()
        } else if (viewportHeight < height) {
            val toViewportSpace = viewportWidth / worldWidth
            val toWorldSpace = worldWidth / viewportWidth
            val lengthen = (height - viewportHeight) * toWorldSpace
            worldHeight += lengthen
            viewportHeight += (lengthen * toViewportSpace).roundToInt()
        }

        virtualWidth = worldWidth
        virtualHeight = worldHeight
        set((width - viewportWidth) / 2, (height - viewportHeight) / 2, viewportWidth, viewportHeight)
        apply(context, centerCamera)
    }
}