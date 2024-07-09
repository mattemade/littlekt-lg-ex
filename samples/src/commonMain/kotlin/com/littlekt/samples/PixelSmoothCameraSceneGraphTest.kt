package com.littlekt.samples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readLDtkMapLoader
import com.littlekt.file.vfs.readTexture
import com.littlekt.graph.node.FrameBufferNode
import com.littlekt.graph.node.canvasLayer
import com.littlekt.graph.node.frameBuffer
import com.littlekt.graph.node.node2d.camera2d
import com.littlekt.graph.node.node2d.node2d
import com.littlekt.graph.node.render.Material
import com.littlekt.graph.node.ui.button
import com.littlekt.graph.node.ui.frameBufferContainer
import com.littlekt.graph.node.ui.label
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.input.Key
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.math.floor
import com.littlekt.math.geom.degrees
import com.littlekt.math.nextPowerOfTwo
import com.littlekt.samples.shaders.PixelSmoothFragmentShader
import com.littlekt.samples.shaders.PixelSmoothVertexShader
import com.littlekt.util.seconds
import kotlin.math.floor

/**
 * @author Colton Daily
 * @date 2/24/2022
 */
class PixelSmoothCameraSceneGraphTest(context: Context) : ContextListener(context) {
    var pxWidth = 0
    var pxHeight = 0
    val targetHeight = 160

    override suspend fun Context.start() {
        val mapLoader = resourcesVfs["ldtk/world-1.5.3.ldtk"].readLDtkMapLoader()
        val icon = resourcesVfs["icon_16x16.png"].readTexture()
        val world = mapLoader.loadLevel(0)

        val pixelSmoothShader =
            ShaderProgram(PixelSmoothVertexShader(), PixelSmoothFragmentShader()).also { it.prepare(this) }

        val graph = sceneGraph(context) {
            ppu = 16f
            val fbo: FrameBufferNode
            var scaledDistX = 0f
            var scaledDistY = 0f
            var subpixelX = 0f
            var subPixelY = 0f
            canvasLayer {
                fbo = frameBuffer {
                    onResize += { width, height ->
                        pxHeight = height / (height / targetHeight)
                        pxWidth = (width / (height / pxHeight))
                        resizeFbo(pxWidth.nextPowerOfTwo, pxHeight.nextPowerOfTwo)
                        canvasCamera.ortho(this@frameBuffer.width * ppuInv, this@frameBuffer.height * ppuInv)
                        canvasCamera.update()
                    }

                    node2d {
                        onRender += { batch, camera, shapeRenderer ->
                            world.render(batch, camera, 0f, 0f, ppuInv)
                            batch.draw(
                                icon,
                                0f,
                                0f,
                                scaleX = ppuInv,
                                scaleY = ppuInv,
                                rotation = 45.degrees
                            )
                        }
                    }


                    camera2d {
                        active = true
                        val cameraDir = MutableVec2f()
                        val targetPosition = MutableVec2f()
                        val velocity = MutableVec2f()
                        val tempVec2f = MutableVec2f()
                        var useBilinearFilter = false
                        val speed = 1f

                        onUpdate += {
                            cameraDir.set(0f, 0f)
                            if (input.isKeyPressed(Key.W)) {
                                cameraDir.y = -1f
                            } else if (input.isKeyPressed(Key.S)) {
                                cameraDir.y = 1f
                            }

                            if (input.isKeyPressed(Key.D)) {
                                cameraDir.x = 1f
                            } else if (input.isKeyPressed(Key.A)) {
                                cameraDir.x = -1f
                            }

                            tempVec2f.set(cameraDir).norm().scale(speed)
                            velocity.mulAdd(tempVec2f, dt.seconds * speed)
                            velocity.lerp(Vec2f.ZERO, 0.7f * (1f - cameraDir.norm().length()))

                            targetPosition += velocity

                            val tx = (targetPosition.x * ppu).floor() / ppu
                            val ty = (targetPosition.y * ppu).floor() / ppu

                            scaledDistX = (targetPosition.x - tx) * ppu
                            scaledDistY = (targetPosition.y - ty) * ppu

                            subpixelX = 0f
                            subPixelY = 0f

                            if (useBilinearFilter) {
                                subpixelX = scaledDistX - floor(scaledDistX)
                                subPixelY = scaledDistY - floor(scaledDistY)
                            }

                            scaledDistX -= subpixelX
                            scaledDistY -= subPixelY

                            (parent as? FrameBufferNode)?.let {
                                globalX = tx + it.width * ppuInv / 2f
                                globalY = ty + it.height * ppuInv / 2f

                                // update the camera position immediately
                                it.canvasCamera.position.set(globalX, globalY, 0f)

                                tempVec2f.x = input.x.toFloat()
                                tempVec2f.y = input.y.toFloat()
                                tempVec2f.x = (pxWidth / 100f) * ((100f / graphics.width) * input.x)
                                tempVec2f.y = (pxHeight / 100f) * ((100f / graphics.height) * input.y)
                                tempVec2f.x *= ppuInv
                                tempVec2f.y *= ppuInv
                                tempVec2f.x = tempVec2f.x - it.width * ppuInv * 0.5f + globalX
                                tempVec2f.y = tempVec2f.y - it.height * ppuInv * 0.5f + globalY
                            }
                            if (input.isKeyJustPressed(Key.B)) {
                                useBilinearFilter = !useBilinearFilter
                            }

                            if (input.isKeyJustPressed(Key.L)) {
                                println(tempVec2f)
                            }
                        }
                    }
                }
            }

            node2d {
                var slice: TextureSlice? = null
                material = Material(pixelSmoothShader)

                fbo.onFboChanged.connect(this) {
                    slice = TextureSlice(it, 0, it.height - pxHeight, pxWidth, pxHeight)
                }

                onRender += { batch, camera, shapeRenderer ->
                    slice?.let {
                        pixelSmoothShader.vertexShader.uTextureSizes.apply(
                            pixelSmoothShader,
                            fbo.width.toFloat(),
                            fbo.height.toFloat(),
                            0f,
                            0f
                        )
                        pixelSmoothShader.vertexShader.uSampleProperties.apply(
                            pixelSmoothShader,
                            subpixelX,
                            subPixelY,
                            scaledDistX,
                            scaledDistY
                        )
                        batch.draw(
                            it,
                            globalX,
                            globalY,
                            width = width,
                            height = height,
                            scaleX = globalScaleX,
                            scaleY = globalScaleY,
                            rotation = globalRotation,
                            flipY = true
                        )
                    }
                }
            }
        }.also { it.initialize() }


        onResize { width, height ->
            graph.resize(width, height)
        }

        onRender { dt ->
            gl.clearColor(Color.DARK_GRAY)
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
