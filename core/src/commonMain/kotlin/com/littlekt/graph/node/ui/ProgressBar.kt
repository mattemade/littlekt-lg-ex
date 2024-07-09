package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.Drawable
import com.littlekt.graph.node.resource.Theme
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.MutableColor
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.font.BitmapFont
import com.littlekt.graphics.g2d.font.BitmapFontCache
import com.littlekt.graphics.g2d.font.GlyphLayout
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.geom.Angle
import com.littlekt.util.toString
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max

/**
 * Adds a [ProgressBar] to the current [Node] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.progressBar(callback: @SceneGraphDslMarker ProgressBar.() -> Unit = {}): ProgressBar {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return ProgressBar().also(callback).addTo(this)
}

/**
 * Adds a [ProgressBar] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.progressBar(callback: @SceneGraphDslMarker ProgressBar.() -> Unit = {}): ProgressBar {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.progressBar(callback)
}


/**
 * General-purpose progress bar. Shows fill percentage from right to left.
 * @author Colton Daily
 * @date 2/6/2022
 */
open class ProgressBar : Range() {

    /**
     * If `true`, the fill percentage is displayed on the bar
     */
    var percentVisible: Boolean = true

    /**
     * Font used to draw the fill percentage if [percentVisible] is `true`.
     */
    var font: BitmapFont
        get() = getThemeFont(themeVars.font)
        set(value) {
            fontOverrides[themeVars.font] = value
            cache = BitmapFontCache(value)
        }

    /**
     * Color of the text.
     */
    var fontColor: Color
        get() = getThemeColor(themeVars.fontColor)
        set(value) {
            colorOverrides[themeVars.fontColor] = value
        }

    /**
     * The background drawable
     */
    var bg: Drawable
        get() = getThemeDrawable(themeVars.bg)
        set(value) {
            drawableOverrides[themeVars.bg] = value
        }

    /**
     * The drawable of the progress (the part that fills the bar).
     */
    var fg: Drawable
        get() = getThemeDrawable(themeVars.fg)
        set(value) {
            drawableOverrides[themeVars.fg] = value
        }


    private var previousValue = -1f
    private var cache: BitmapFontCache = BitmapFontCache(font)
    private val layout = GlyphLayout()

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        bg.draw(batch, globalX, globalY, width, height)
        val progress = ratio * (width - fg.minWidth)
        if (progress > 0) {
            fg.draw(batch, globalX, globalY, progress + fg.minWidth, height)
        }

        if (percentVisible) {
            if (previousValue != value) {
                val text = (ratio * 100.0).toString(1) + "%"
                layout.setText(font, text, fontColor)
                cache.setText(layout, 0f, height / 2f - font.capHeight)
                previousValue = value
            }

            tempColor.set(color).mul(fontColor)
            cache.tint(tempColor)

            if (globalRotation != Angle.ZERO || globalScaleX != 1f || globalScaleY != 1f) {
                applyTransform(batch)
                cache.setPosition(0f, 0f)
                cache.draw(batch)
                resetTransform(batch)
            } else {
                cache.setPosition(globalX + width / 2f - layout.width / 2f, globalY)
                cache.draw(batch)
            }
        }
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        var minHeight = max(bg.minHeight, fg.minHeight)
        var minWidth = max(bg.minWidth, fg.minWidth)

        if (percentVisible) {
            minSizeLayout.setText(font, "100%")
            minHeight = max(minHeight, bg.minHeight + minSizeLayout.height)
        } else {
            // needed or else the progress bar will collapse
            minWidth = max(minWidth, 1f)
            minHeight = max(minHeight, 1f)
        }

        _internalMinWidth = minWidth
        _internalMinHeight = minHeight

        minSizeInvalid = false
    }

    class ThemeVars {
        val bg = "bg"
        val fg = "fg"
        val font = "font"
        val fontColor = "fontColor"
    }

    companion object {
        /**
         * [Theme] related variable names when setting theme values for a [PaddedContainer]
         */
        val themeVars = ThemeVars()

        private val tempColor = MutableColor()
        private val minSizeLayout = GlyphLayout()
    }

}