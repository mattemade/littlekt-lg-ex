package com.littlekt.samples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.node
import com.littlekt.graph.node.node2d.Node2D
import com.littlekt.graph.node.node2d.node2d
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.toFloatBits
import com.littlekt.input.Key
import com.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 7/24/2022
 */
class YSortTest(context: Context) : ContextListener(context) {
    private val batch = SpriteBatch(context)

    inner class TestNode : Node2D() {
        var color = Color.WHITE

        override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
            super.render(batch, camera, shapeRenderer)
            val bits = color.toFloatBits()
            shapeRenderer.filledRectangle(globalX, globalY, 8f, 16f, rotation, bits)

        }
    }

    fun SceneGraph<*>.init() {

        val input = context.input

        node(TestNode()) {
            color = Color.YELLOW
            x = 100f
            y = 35f
        }
        node(TestNode()) {
            color = Color.YELLOW
            x = 150f
            y = 35f
        }
        node(TestNode()) {
            color = Color.YELLOW
            x = 200f
            y = 35f
        }
        node2d {
            ySort = true

            // show draw  blue -> green -> red
            node(TestNode()) {
                color = Color.RED
                y = 50f
                x = 100f
            }

            node(TestNode()) {
                color = Color.GREEN
                y = 45f
                x = 100f
            }

            node(TestNode()) {
                color = Color.BLUE
                y = 40f
                x = 100f
            }


            // show draw red -> blue -> green
            // pressing enter should reorder to green -> red -> blue
            node(TestNode()) {
                color = Color.RED
                y = 40f
                x = 150f

                onUpdate += {
                    if (input.isKeyPressed(Key.SPACE)) {
                        println("RED!!")
                    }
                    if (input.isKeyJustPressed(Key.ENTER)) {
                        y = 45f
                    }
                }
            }

            node(TestNode()) {
                color = Color.GREEN
                y = 50f
                y = 50f
                x = 150f
                onUpdate += {
                    if (input.isKeyPressed(Key.SPACE)) {
                        println("GREEN!!")
                    }
                    if (input.isKeyJustPressed(Key.ENTER)) {
                        y = 40f
                    }
                }
            }

            node(TestNode()) {
                color = Color.BLUE
                y = 45f
                x = 150f
                onUpdate += {
                    if (input.isKeyJustPressed(Key.ENTER)) {
                        y = 50f
                    }
                }
            }

            // show draw red -> green -> blue
            node(TestNode()) {
                color = Color.RED
                y = 40f
                x = 200f
            }

            node(TestNode()) {
                color = Color.GREEN
                y = 45f
                x = 200f
            }

            node(TestNode()) {
                color = Color.BLUE
                y = 50f
                x = 200f
            }
        }

        node(TestNode()) {
            color = Color.LIGHT_GRAY
            x = 100f
            y = 55f
        }

        node(TestNode()) {
            color = Color.LIGHT_GRAY
            x = 150f
            y = 55f
        }

        node(TestNode()) {
            color = Color.LIGHT_GRAY
            x = 200f
            y = 55f
        }
    }

    override suspend fun Context.start() {
        val graph = sceneGraph(context, ExtendViewport(480, 270), batch) {
            init()
        }.also { it.initialize() }

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