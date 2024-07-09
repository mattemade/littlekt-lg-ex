package com.littlekt.samples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readBitmapFont
import com.littlekt.file.vfs.readTexture
import com.littlekt.graph.node.frameBuffer
import com.littlekt.graph.node.node2d.camera2d
import com.littlekt.graph.node.node2d.node2d
import com.littlekt.graph.node.resource.HAlign
import com.littlekt.graph.node.ui.button
import com.littlekt.graph.node.ui.centerContainer
import com.littlekt.graph.node.ui.frameBufferContainer
import com.littlekt.graph.node.ui.label
import com.littlekt.graph.node.viewport
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.input.Key
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.degrees
import com.littlekt.math.geom.radians
import com.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 2/24/2022
 */
class CanvasCameraTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val pixelFont = resourcesVfs["m5x7_16.fnt"].readBitmapFont()
        val icon = resourcesVfs["icon_16x16.png"].readTexture()
        val graph = sceneGraph(context, ExtendViewport(480, 270)) {
                frameBufferContainer {
                    stretch = true
                    shrink = 2
                    anchorRight = 1f
                    anchorBottom = 1f

                    frameBuffer {
                        button {
                            text = "test"
                        }
                        node2d {
                            rotation = 45.degrees
                            onReady += {
                                println("$name: $canvas")
                            }
                            onUpdate += {
                                if (input.isKeyPressed(Key.D)) {
                                    globalX += 1f
                                } else if (input.isKeyPressed(Key.A)) {
                                    globalX -= 1f
                                }

                                if (input.isKeyPressed(Key.S)) {
                                    globalY += 1f
                                } else if (input.isKeyPressed(Key.W)) {
                                    globalY -= 1f
                                }
                            }
                            onRender += { batch, camera, shapeRenderer ->
                                batch.draw(icon, globalX, globalY, rotation = globalRotation)
                            }
                            camera2d {
                                active = true
                            }
                        }

                        var rotation = Angle.ZERO
                        node2d {
                            x = 100f
                            y = 20f
                            onRender += { batch, camera, shapeRenderer ->
                                rotation += 0.01.radians
                                batch.draw(icon, globalX, globalY, scaleX = 2f, scaleY = 2f, rotation = rotation)
                            }
                        }
                    }
                }

            centerContainer {
                anchorRight = 1f
                anchorBottom = 1f
                label {
                    text = "Should be centered label"
                    horizontalAlign = HAlign.CENTER
                    font = pixelFont

                    onReady += {
                        println("$name: ${canvas!!::class.simpleName} - $canvas")
                    }
                }
            }

        }.also {
            it.initialize()
            println(it.sceneCanvas.treeString())
        }

        graph.requestShowDebugInfo = true
        onResize { width, height ->
            graph.resize(width, height, true)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            graph.update(dt)
            graph.render()

            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}