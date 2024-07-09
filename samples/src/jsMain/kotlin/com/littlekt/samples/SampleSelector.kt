package com.littlekt.samples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.LittleKtApp
import com.littlekt.createLittleKtApp
import com.littlekt.graphics.Color
import kotlinx.browser.document
import kotlinx.html.DIV
import kotlinx.html.button
import kotlinx.html.dom.append
import kotlinx.html.id
import kotlinx.html.js.canvas
import kotlinx.html.js.div
import kotlinx.html.js.onClickFunction
import kotlinx.html.style

/**
 * @author Colton Daily
 * @date 11/22/2021
 */

var lastApp: LittleKtApp? = null

fun main() {
    document.body!!.append {
        div {
            addSample("Display Test") { DisplayTest(it) }
            addSample("Mutable Atlas Test") { MutableAtlasTest(it) }
            addSample("Texture Array Sprite Batch Test") { TextureArraySpriteBatchTest(it) }
            addSample("Tiled Map Test") { TiledMapTest(it) }
            addSample("LDtk Map Test") { LDtkMapTest(it) }
            addSample("Pixel Smooth Camera Test") { PixelSmoothCameraTest(it) }
            addSample("Gesture Controller Test") { GestureControllerTest(it) }
            addSample("FBO Multi Target Test") { FBOMultiTargetTest(it) }
            addSample("Shaders Console output Test") { ShadersTest(it) }
        }
    }
}

fun DIV.addSample(title: String, gameBuilder: (app: Context) -> ContextListener) {
    button {
        +title
        onClickFunction = {
            document.getElementById("canvas")?.remove()
            document.getElementById("canvas-container")!!.append {
                canvas {
                    id = "canvas"
                    width = "960"
                    height = "540"
                    style = "border:1px solid #000000;"
                }
            }

            lastApp?.close()
            lastApp = createLittleKtApp {
                this.title = title
                backgroundColor = Color.DARK_GRAY
            }.also {
                it.start(gameBuilder)
            }
        }
    }
}