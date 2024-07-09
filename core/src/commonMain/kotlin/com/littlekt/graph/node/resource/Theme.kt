package com.littlekt.graph.node.resource

import com.littlekt.graph.node.ui.*
import com.littlekt.graphics.Color
import com.littlekt.graphics.Fonts
import com.littlekt.graphics.g2d.NinePatch
import com.littlekt.graphics.Textures

/**
 * @author Colton Daily
 * @date 1/19/2022
 */
class Theme(
    /**
     * A map of drawables mapped by Node type, mapped by variable name and value.
     *
     * Eg: `drawables["Button"]["pressed"]`
     */
    val drawables: Map<String, Map<String, Drawable>> = mapOf(),

    /**
     * A map of fonts mapped by Node type, mapped by variable name and value.
     *
     * Eg: `fonts["Button"]["font"]`
     */
    val fonts: Map<String, Map<String, com.littlekt.graphics.g2d.font.BitmapFont>> = mapOf(),

    /**
     * A map of colors mapped by Node type, mapped by variable name and value.
     *
     * Eg: `colors["Button"]["fontColor"]`
     */
    val colors: Map<String, Map<String, Color>> = mapOf(),

    /**
     * A map of constants mapped by Node type, mapped by variable name and value.
     *
     * constants["Button"]["myVar"]
     */
    val constants: Map<String, Map<String, Int>> = mapOf(),
    val defaultFont: com.littlekt.graphics.g2d.font.BitmapFont? = null,
) {

    companion object {
        val FALLBACK_DRAWABLE = emptyDrawable()
        val FALLBACK_FONT = Fonts.default

        private var _theme: Theme? = null
        var defaultTheme: Theme
            get() {
                if (_theme == null) {
                    _theme = createDefaultTheme()
                }
                return _theme!!
            }
            set(value) {
                _theme = value
            }
    }
}

/**
 * Creates a new [Theme] using the default theme values and allowing to add or override any additional theme values.
 * @return the newly created theme
 */
fun createDefaultTheme(
    /**
     * A map of drawables mapped by Node type, mapped by variable name and value.
     *
     * Eg: `drawables["Button"]["pressed"]`
     */
    extraDrawables: Map<String, Map<String, Drawable>> = mapOf(),

    /**
     * A map of fonts mapped by Node type, mapped by variable name and value.
     *
     * Eg: `fonts["Button"]["font"]`
     */
    extraFonts: Map<String, Map<String, com.littlekt.graphics.g2d.font.BitmapFont>> = mapOf(),

    /**
     * A map of colors mapped by Node type, mapped by variable name and value.
     *
     * Eg: `colors["Button"]["fontColor"]`
     */
    extraColors: Map<String, Map<String, Color>> = mapOf(),

    /**
     * A map of constants mapped by Node type, mapped by variable name and value.
     *
     * constants["Button"]["myVar"]
     */
    extraConstants: Map<String, Map<String, Int>> = mapOf(),
    defaultFont: com.littlekt.graphics.g2d.font.BitmapFont? = null,
): Theme {
    val greyButtonNinePatch = NinePatch(
        Textures.atlas.getByPrefix("grey_button").slice,
        5,
        5,
        5,
        4
    )
    val greyOutlineNinePatch = NinePatch(
        Textures.atlas.getByPrefix("grey_outline").slice,
        2,
        2,
        2,
        2
    )
    val panelNinePatch = NinePatch(
        Textures.atlas.getByPrefix("grey_panel").slice,
        6,
        6,
        6,
        6
    )

    val greyBoxNinePatch = NinePatch(
        Textures.atlas.getByPrefix("grey_box").slice,
        7,
        7,
        6,
        6
    )

    val greySliderBg = NinePatch(
        Textures.atlas.getByPrefix("grey_sliderBg").slice,
        3,
        3,
        3,
        3
    )

    val grayGrabber = NinePatch(
        Textures.atlas.getByPrefix("grey_grabber").slice,
        3,
        3,
        3,
        3
    )
    val darkBlue = Color.fromHex("242b33")
    val lightBlue = Color.fromHex("3d4754")

    val drawables = mapOf(
        "Button" to mapOf(
            Button.themeVars.normal to NinePatchDrawable(greyButtonNinePatch)
                .apply { modulate = lightBlue },
            Button.themeVars.normal to NinePatchDrawable(greyButtonNinePatch)
                .apply { modulate = lightBlue },
            Button.themeVars.pressed to NinePatchDrawable(greyButtonNinePatch)
                .apply { modulate = lightBlue.toMutableColor().scaleRgb(0.6f) },
            Button.themeVars.hover to NinePatchDrawable(greyButtonNinePatch)
                .apply { modulate = lightBlue.toMutableColor().lighten(0.2f) },
            Button.themeVars.disabled to NinePatchDrawable(greyButtonNinePatch)
                .apply { modulate = lightBlue.toMutableColor().lighten(0.5f) },
            Button.themeVars.focus to NinePatchDrawable(greyOutlineNinePatch)
                .apply { modulate = Color.WHITE },
        ),
        "Panel" to mapOf(
            Panel.themeVars.panel to NinePatchDrawable(panelNinePatch).apply {
                modulate = lightBlue
            }
        ),
        "ProgressBar" to mapOf(
            ProgressBar.themeVars.bg to NinePatchDrawable(greySliderBg).apply {
                modulate = darkBlue
            },
            ProgressBar.themeVars.fg to NinePatchDrawable(greySliderBg).apply {
                modulate = lightBlue.toMutableColor().lighten(0.5f)
            }
        ),
        "LineEdit" to mapOf(
            LineEdit.themeVars.bg to NinePatchDrawable(greyBoxNinePatch).apply {
                minWidth = 50f
                minHeight = 25f
                modulate = darkBlue
            },
            LineEdit.themeVars.disabled to NinePatchDrawable(greyBoxNinePatch).apply {
                minWidth = 50f
                minHeight = 25f
                modulate = darkBlue.toMutableColor().lighten(0.2f)
            },
            LineEdit.themeVars.caret to TextureSliceDrawable(Textures.white).apply {
                minWidth = 1f
            },
            LineEdit.themeVars.selection to TextureSliceDrawable(Textures.white).apply {
                modulate = lightBlue
            },
            LineEdit.themeVars.focus to NinePatchDrawable(greyOutlineNinePatch)
                .apply { modulate = Color.WHITE },
        ),
        "ScrollContainer" to mapOf(ScrollContainer.themeVars.panel to emptyDrawable()),
        "VScrollBar" to mapOf(
            ScrollBar.themeVars.scroll to NinePatchDrawable(greySliderBg).apply {
                modulate = darkBlue
            },
            ScrollBar.themeVars.grabber to NinePatchDrawable(grayGrabber).apply {
                modulate = lightBlue
            },
            ScrollBar.themeVars.grabberPressed to NinePatchDrawable(grayGrabber).apply {
                modulate = lightBlue.toMutableColor().scaleRgb(0.6f)
            },
            ScrollBar.themeVars.grabberHighlight to NinePatchDrawable(grayGrabber).apply {
                modulate = lightBlue.toMutableColor().lighten(0.2f)
            }
        ),
        "HScrollBar" to mapOf(
            ScrollBar.themeVars.scroll to NinePatchDrawable(greySliderBg).apply {
                modulate = darkBlue
            },
            ScrollBar.themeVars.grabber to NinePatchDrawable(grayGrabber).apply {
                modulate = lightBlue
            },
            ScrollBar.themeVars.grabberPressed to NinePatchDrawable(grayGrabber).apply {
                modulate = lightBlue.toMutableColor().scaleRgb(0.6f)
            },
            ScrollBar.themeVars.grabberHighlight to NinePatchDrawable(grayGrabber).apply {
                modulate = lightBlue.toMutableColor().lighten(0.2f)
            })
    ) + extraDrawables

    val fonts = extraFonts

    val colors = mapOf(
        "Button" to mapOf(Button.themeVars.fontColor to Color.WHITE),
        "Label" to mapOf(Label.themeVars.fontColor to Color.WHITE),
        "LineEdit" to mapOf(
            LineEdit.themeVars.fontColor to Color.WHITE,
            LineEdit.themeVars.fontColorPlaceholder to Color.LIGHT_GRAY,
            LineEdit.themeVars.fontColorDisabled to Color.LIGHT_GRAY
        )
    ) + extraColors

    val constants = extraConstants

    return Theme(
        drawables = drawables,
        fonts = fonts,
        colors = colors,
        constants = constants,
        defaultFont = defaultFont
    )
}