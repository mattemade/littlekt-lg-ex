package com.littlekt.samples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graph.node.ui.button
import com.littlekt.graph.node.ui.label
import com.littlekt.graph.node.viewport
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.input.Key
import com.littlekt.math.Vec2f
import com.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 1/22/2023
 */
class SceneGraphNestedViewportsTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val graph = sceneGraph(context, ExtendViewport(960, 540)) {
            viewport {
                name = "Nested Viewport"
                viewport = ExtendViewport(480, 270)
                label {
                    marginTop = 80f
                    marginLeft = 5f
                    name = "Nested"
                    onInput += { event ->
                        text =
                            "$name: canvas coords: ${event.canvasX},${event.canvasY} local: ${event.localX},${event.localY}"
                    }
                }
                button {
                    marginTop = 100f
                    marginLeft = 5f
                    text = "Nested"
                }

                viewport {
                    name = "Double Nested Viewport"
                    viewport = ExtendViewport(240, 135)
                    label {
                        marginTop = 80f
                        marginLeft = 5f
                        name = "Double Nested"
                        fontScale = Vec2f(0.5f)
                        onInput += { event ->
                            text =
                                "$name: canvas coords: ${event.canvasX},${event.canvasY} local: ${event.localX},${event.localY}"
                        }
                    }

                    button {
                        marginTop = 100f
                        marginLeft = 5f
                        text = "Double nested"
                    }
                }
            }

            label {
                marginTop = 10f
                marginLeft = 10f
                name = "Root"
                onInput += { event ->
                    text =
                        "$name: canvas coords: ${event.canvasX},${event.canvasX} local: ${event.localX},${event.localY}"
                }
            }

            button {
                marginTop = 30f
                marginLeft = 5f
                text = "Root"
            }

        }
        graph.initialize()
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

            if (input.isKeyJustPressed(Key.T)) {
                println(graph.root.treeString())
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}