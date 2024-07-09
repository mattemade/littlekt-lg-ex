package com.littlekt.samples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readBitmapFont
import com.littlekt.file.vfs.readTexture
import com.littlekt.graph.node.resource.HAlign
import com.littlekt.graph.node.ui.hBoxContainer
import com.littlekt.graph.node.ui.label
import com.littlekt.graph.node.ui.scrollContainer
import com.littlekt.graph.node.ui.vBoxContainer
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.Fonts
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.input.InputMapController
import com.littlekt.input.Key
import com.littlekt.input.Pointer
import com.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 2/24/2022
 */
class UiTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val batch = SpriteBatch(this)
        val pixelFont = resourcesVfs["m5x7_16.fnt"].readBitmapFont()
        val icon = resourcesVfs["icon_16x16.png"].readTexture()
        val viewport = ExtendViewport(480, 270)
        val camera = viewport.camera

        val controller = InputMapController<String>(input)
        controller.addBinding("reset", listOf(Key.R, Key.T), keyModifiers = listOf(InputMapController.KeyModifier.SHIFT))
        controller.addBinding("test", listOf(Key.T))
        controller.addBinding("pressed", pointers = listOf(Pointer.MOUSE_LEFT))

        input.addInputProcessor(controller)

        val graph = sceneGraph(context, ExtendViewport(480, 270)) {

//            centerContainer {
//                anchorRight = 1f
//                anchorBottom = 1f
//
//                vBoxContainer {
//                    separation = 5
//
//                    label {
//                        text = "A"
//                        horizontalAlign = HAlign.CENTER
//                        font = pixelFont
//                    }
//                    label {
//                        text = "My Label Middle"
//                        horizontalAlign = HAlign.CENTER
//                        font = pixelFont
//                    }
//                    textureRect {
//                        slice = icon.slice()
//                        stretchMode = TextureRect.StretchMode.KEEP_CENTERED
//                    }
//
//                }
//            }

            scrollContainer {
                x = 50f
                y = 50f
                width = 200f
                height = 150f

                vBoxContainer {
                    repeat(50) { i ->
                        hBoxContainer {
                            repeat(15) {
                                label {
                                    text = "$it$it$it$it$it$it$it$it$it"
                                    if (it == 0) text = "$i-$text"
                                }
                            }
                        }
                    }
                }
            }
        }.also { it.initialize() }


        onResize { width, height ->
            viewport.update(width, height, this)
            graph.resize(width, height, true)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            camera.update()
            viewport.apply(this)
            batch.use(camera.viewProjection) {
                Fonts.default.draw(it, "My Label\nMiddle", 0f, 0f, targetWidth = 480f, align = HAlign.CENTER)
                Fonts.default.draw(it, "FPS: ${context.stats.fps.toInt()}", 35f, 50f)
            }

            if(controller.down("pressed")) {
                println("POINTER DOWN")
            }

            if(controller.pressed("pressed")) {
                println("POINTER PRESSED")
            }

            if(controller.released("pressed")) {
                println("POINTER RELEASED")
            }

            graph.update(dt)
            graph.render()

            if (input.isKeyJustPressed(Key.ENTER)) {
                graph.requestShowDebugInfo = !graph.requestShowDebugInfo
            }

            if (input.isKeyJustPressed(Key.T)) {
        //        logger.info { "\n" + graph.root.treeString() }
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