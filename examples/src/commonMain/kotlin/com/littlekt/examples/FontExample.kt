package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readBitmapFont
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.*
import com.littlekt.resources.Fonts
import com.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 4/17/2024
 */
class FontExample(context: Context) : ContextListener(context) {
    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        val pixelFont = resourcesVfs["m5x7_32.fnt"].readBitmapFont()
        val arialFont = resourcesVfs["arial_32.fnt"].readBitmapFont()
        val device = graphics.device

        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat

        graphics.configureSurface(
            TextureUsage.RENDER_ATTACHMENT,
            preferredFormat,
            PresentMode.FIFO,
            surfaceCapabilities.alphaModes[0]
        )

        val batch = SpriteBatch(device, graphics, preferredFormat)
        val viewport = ExtendViewport(960, 540)
        val camera = viewport.camera

        onResize { width, height ->
            viewport.update(width, height)
            graphics.configureSurface(
                TextureUsage.RENDER_ATTACHMENT,
                preferredFormat,
                PresentMode.FIFO,
                surfaceCapabilities.alphaModes[0]
            )
        }

        addWASDMovement(camera, 0.05f)
        onUpdate {
            val surfaceTexture = graphics.surface.getCurrentTexture()
            when (val status = surfaceTexture.status) {
                TextureStatus.SUCCESS -> {
                    // all good, could check for `surfaceTexture.suboptimal` here.
                }
                TextureStatus.TIMEOUT,
                TextureStatus.OUTDATED,
                TextureStatus.LOST -> {
                    surfaceTexture.texture?.release()
                    logger.info { "getCurrentTexture status=$status" }
                    return@onUpdate
                }
                else -> {
                    // fatal
                    logger.fatal { "getCurrentTexture status=$status" }
                    close()
                    return@onUpdate
                }
            }
            val swapChainTexture = checkNotNull(surfaceTexture.texture)
            val frame = swapChainTexture.createView()

            val commandEncoder = device.createCommandEncoder()
            val renderPassEncoder =
                commandEncoder.beginRenderPass(
                    desc =
                        RenderPassDescriptor(
                            listOf(
                                RenderPassColorAttachmentDescriptor(
                                    view = frame,
                                    loadOp = LoadOp.CLEAR,
                                    storeOp = StoreOp.STORE,
                                    clearColor =
                                        if (preferredFormat.srgb) Color.DARK_GRAY.toLinear()
                                        else Color.DARK_GRAY
                                )
                            )
                        )
                )
            camera.update()

            batch.use(renderPassEncoder, camera.viewProjection) {
                pixelFont.draw(it, "Hello\nLittleKt!", 0f, 0f, align = HAlign.CENTER)
                arialFont.draw(it, "Hello\nLittleKt!", -300f, 0f)
                Fonts.default.draw(it, "Hello\nLittleKt!", 150f, 0f, align = HAlign.RIGHT)
            }

            renderPassEncoder.end()

            val commandBuffer = commandEncoder.finish()

            device.queue.submit(commandBuffer)
            graphics.surface.present()

            commandBuffer.release()
            renderPassEncoder.release()
            commandEncoder.release()
            frame.release()
            swapChainTexture.release()
        }

        onRelease { batch.release() }
    }
}
