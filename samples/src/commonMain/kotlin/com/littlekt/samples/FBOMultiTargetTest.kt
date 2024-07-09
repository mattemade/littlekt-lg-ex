package com.littlekt.samples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.createShader
import com.littlekt.file.createIntBuffer
import com.littlekt.graphics.*
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.gl.ClearBufferMask
import com.littlekt.graphics.gl.TexMagFilter
import com.littlekt.graphics.gl.TexMinFilter
import com.littlekt.graphics.shader.FragmentShaderModel
import com.littlekt.graphics.shader.ShaderParameter
import com.littlekt.graphics.shader.generator.Precision
import com.littlekt.graphics.shader.generator.type.sampler.Sampler2D
import com.littlekt.graphics.shader.generator.type.vec.Vec2
import com.littlekt.graphics.shader.generator.type.vec.Vec4
import com.littlekt.graphics.shader.shaders.DefaultVertexShader
import com.littlekt.input.Key
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.radians
import com.littlekt.util.milliseconds
import com.littlekt.util.viewport.ScreenViewport
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 1/26/2024
 */
class FBOMultiTargetTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val batch = SpriteBatch(this)
        val shader = createShader(DefaultVertexShader(), TestMultiTargetFragmentShader())

        val viewport = ScreenViewport(context.graphics.width, context.graphics.height)
        val camera = viewport.camera

        val fboCamera = OrthographicCamera(240, 135).apply {
            position.set(240 / 2f, 135 / 2f, 0f)
        }
        val fbo = FrameBuffer(
            240,
            135,
            listOf(
                FrameBuffer.TextureAttachment(minFilter = TexMinFilter.NEAREST, magFilter = TexMagFilter.NEAREST),
                FrameBuffer.TextureAttachment(minFilter = TexMinFilter.NEAREST, magFilter = TexMagFilter.NEAREST)
            )
        ).also {
            it.prepare(context)
        }
        val slice = fbo.textures[0].slice()
        val slice2 = fbo.textures[1].slice()
        val buffersToDraw = createIntBuffer(2).apply {
            put(GL.COLOR_ATTACHMENT0)
            put(GL.COLOR_ATTACHMENT1)
            flip()
        }

        var x = 0f
        var y = 0f
        var rotation = Angle.ZERO
        onResize { width, height ->
            viewport.update(width, height, this, true)
        }

        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            val speed = 0.2f * dt.milliseconds
            if (input.isKeyPressed(Key.W)) {
                y -= speed
            }
            if (input.isKeyPressed(Key.S)) {
                y += speed
            }
            if (input.isKeyPressed(Key.D)) {
                x += speed
            }
            if (input.isKeyPressed(Key.A)) {
                x -= speed
            }
            rotation += 0.01.radians

            if (abs(fboCamera.position.x - x) >= 25) {
                fboCamera.position.x = x
            }
            if (abs(fboCamera.position.y - y) >= 25) {
                fboCamera.position.y = y
            }
            fboCamera.update()
            fbo.use {
                gl.drawBuffers(2, buffersToDraw)
                gl.clearColor(0.5f, 0f, 0f, 1f)
                gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
                batch.shader = shader
                batch.use(fboCamera.viewProjection) {
                    it.draw(Textures.white, x, y, scaleX = 10f, scaleY = 10f, rotation = rotation)
                    it.draw(Textures.white, x + 20f, y + 20f, scaleX = 5f, scaleY = 5f, rotation = rotation)
                }
            }

            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            camera.update()
            batch.useDefaultShader()
            batch.use(camera.viewProjection) {
                var sx = graphics.width / 240f
                var sy = graphics.height / 135f
                sx = floor(sx)
                sy = floor(sy)

                val scale = max(1f, min(sx, sy))
                it.draw(slice, 0f, 0f, scaleX = scale, scaleY = scale, flipY = true)
                it.draw(slice2, 0f, 0f, scaleX = scale * 0.5f, scaleY = scale * 0.5f, flipY = true)
            }


            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }

    private class TestMultiTargetFragmentShader : FragmentShaderModel() {
        val uTexture get() = parameters["u_texture"] as ShaderParameter.UniformSample2D

        private val u_texture by uniform(::Sampler2D)

        private val v_color by varying(::Vec4, Precision.LOW)
        private val v_texCoords by varying(::Vec2)

        init {
            gl_FragData[0] = v_color * texture2D(u_texture, v_texCoords).lit
            gl_FragData[1] = vec4(0f, 1f, 0f, 1f).lit * texture2D(u_texture, v_texCoords).lit
        }
    }
}

