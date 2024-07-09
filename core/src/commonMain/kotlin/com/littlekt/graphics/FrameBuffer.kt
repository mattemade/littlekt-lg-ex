package com.littlekt.graphics

import com.littlekt.Context
import com.littlekt.Disposable
import com.littlekt.graphics.FrameBuffer.TextureAttachment
import com.littlekt.graphics.gl.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Encapsulates OpenGL frame buffer objects.
 * @param width the width of the framebuffer in pixels
 * @param height the height of the framebuffer in pixels
 * @param textureAttachments the list of [TextureAttachment] to attach to the FrameBuffer.
 * @param hasDepth whether to attach a depth buffer. Defaults to false.
 * @param hasStencil whether to attach a stencil buffer. Defaults to false.
 * @param hasPackedDepthStencil whether to attach a packed depth/stencil buffer. Defaults to false.
 * @author Colton Daily
 * @date 11/25/2021
 */
open class FrameBuffer(
    val width: Int,
    val height: Int,
    val textureAttachments: List<TextureAttachment> = listOf(TextureAttachment()),
    val hasDepth: Boolean = false,
    val hasStencil: Boolean = false,
    var hasPackedDepthStencil: Boolean = false,
) : Preparable, Disposable {

    /**
     * Encapsulates OpenGL frame buffer objects.
     * @param width the width of the framebuffer in pixels
     * @param height the height of the framebuffer in pixels
     * @param format format of the color buffer
     * @param hasDepth whether to attach a depth buffer. Defaults to false.
     * @param hasStencil whether to attach a stencil buffer. Defaults to false.
     * @param hasPackedDepthStencil whether to attach a packed depth/stencil buffer. Defaults to false.
     * @param minFilter texture min filter
     * @param magFilter texture mag filter
     * @param wrap format for UV texture wrap
     */
    constructor(
        width: Int,
        height: Int,
        hasDepth: Boolean = false,
        hasStencil: Boolean = false,
        hasPackedDepthStencil: Boolean = false,
        format: Pixmap.Format = Pixmap.Format.RGBA8888,
        minFilter: TexMinFilter = TexMinFilter.LINEAR,
        magFilter: TexMagFilter = TexMagFilter.LINEAR,
        wrap: TexWrap = TexWrap.CLAMP_TO_EDGE,
    ) : this(
        width,
        height,
        listOf(TextureAttachment(format, minFilter, magFilter, wrap)),
        hasDepth,
        hasStencil,
        hasPackedDepthStencil
    )

    /**
     * A color attachment to be used in [FrameBuffer].
     * @param format format of the color buffer
     * @param minFilter texture min filter
     * @param magFilter texture mag filter
     * @param wrap format for UV texture wrap
     * @param isDepth `true` if texture depth attachment; `false` otherwise
     * @param isStencil `true` if texture is a stencil attachment; `false` otherwise
     */
    data class TextureAttachment(
        val format: Pixmap.Format = Pixmap.Format.RGBA8888,
        val minFilter: TexMinFilter = TexMinFilter.LINEAR,
        val magFilter: TexMagFilter = TexMagFilter.LINEAR,
        val wrap: TexWrap = TexWrap.CLAMP_TO_EDGE,
        val isDepth: Boolean = false,
        val isStencil: Boolean = false,
    ) {
        val isColorTexture: Boolean get() = !isDepth && !isStencil
    }

    /**
     * Gets set when the frame buffer is prepared by the application
     */
    private lateinit var gl: GL
    private lateinit var context: Context

    private var fboHandle: GlFrameBuffer? = null
    private var depthBufferHandle: GlRenderBuffer? = null
    private var stencilBufferHandle: GlRenderBuffer? = null
    private var depthStencilPackedBufferHandle: GlRenderBuffer? = null

    private var isPrepared = false

    private val _textures = mutableListOf<Texture>()
    val textures: List<Texture> get() = _textures

    /**
     * Alias for `textures.getOrNull(0)`.
     */
    val texture: Texture? get() = textures.getOrNull(0)

    /**
     * Alias for `textures[0]`.
     */
    val colorBufferTexture: Texture get() = textures[0]

    override val prepared: Boolean
        get() = isPrepared


    override fun prepare(context: Context) {
        gl = context.gl
        this.context = context
        val fboHandle = gl.createFrameBuffer()
        this.fboHandle = fboHandle

        gl.bindFrameBuffer(fboHandle)
        if (hasDepth) {
            depthBufferHandle = gl.createRenderBuffer()
            depthBufferHandle?.let {
                gl.bindRenderBuffer(it)
                gl.renderBufferStorage(RenderBufferInternalFormat.DEPTH_COMPONENT16, width, height)
            }
        }

        if (hasStencil) {
            stencilBufferHandle = gl.createRenderBuffer()
            stencilBufferHandle?.let {
                gl.bindRenderBuffer(it)
                gl.renderBufferStorage(RenderBufferInternalFormat.STENCIL_INDEX8, width, height)
            }
        }

        if (hasPackedDepthStencil) {
            depthStencilPackedBufferHandle = gl.createRenderBuffer()
            depthStencilPackedBufferHandle?.let {
                gl.bindRenderBuffer(it)
                gl.renderBufferStorage(RenderBufferInternalFormat.DEPTH24_STENCIL8, width, height)
            }
        }

        textureAttachments.forEachIndexed { i, attachment ->
            _textures += Texture(
                GLTextureData(
                    width,
                    height,
                    0,
                    attachment.format.glFormat,
                    attachment.format.glFormat,
                    attachment.format.glType
                )
            ).apply {
                minFilter = attachment.minFilter
                magFilter = attachment.magFilter
                uWrap = attachment.wrap
                vWrap = attachment.wrap
            }.also { texture ->
                texture.prepare(context) // preparing the texture will also bind it
                if (attachment.isColorTexture) {
                    gl.frameBufferTexture2D(
                        FrameBufferRenderBufferAttachment.COLOR_ATTACHMENT(i),
                        texture.glTexture
                            ?: throw RuntimeException("FrameBuffer failed on attempting to add color attachment($i)!"),
                        0
                    )
                } else if (attachment.isDepth) {
                    gl.frameBufferTexture2D(
                        FrameBufferRenderBufferAttachment.DEPTH_ATTACHMENT,
                        texture.glTexture
                            ?: throw RuntimeException("FrameBuffer failed on attempting to add depth attachment!"),
                        0
                    )
                } else if (attachment.isStencil) {
                    gl.frameBufferTexture2D(
                        FrameBufferRenderBufferAttachment.STENCIL_ATTACHMENT,
                        texture.glTexture
                            ?: throw RuntimeException("FrameBuffer failed on attempting to add stencil attachment!"),
                        0
                    )
                }
            }
        }

        depthBufferHandle?.let {
            gl.frameBufferRenderBuffer(FrameBufferRenderBufferAttachment.DEPTH_ATTACHMENT, it)
        }

        stencilBufferHandle?.let {
            gl.frameBufferRenderBuffer(FrameBufferRenderBufferAttachment.STENCIL_ATTACHMENT, it)
        }
        depthStencilPackedBufferHandle?.let {
            gl.frameBufferRenderBuffer(FrameBufferRenderBufferAttachment.DEPTH_STENCIL_ATTACHMENT, it)
        }

        gl.bindDefaultRenderBuffer()

        var result = gl.checkFrameBufferStatus()
        if (result == FrameBufferStatus.FRAMEBUFFER_UNSUPPORTED && hasDepth && hasStencil &&
            (context.graphics.supportsExtension("GL_OES_packed_depth_stencil")
                    || context.graphics.supportsExtension("GL_EXT_packed_depth_stencil"))
        ) {
            if (hasDepth) {
                depthBufferHandle?.let {
                    gl.deleteRenderBuffer(it)
                }
                depthBufferHandle = null
            }
            if (hasStencil) {
                stencilBufferHandle?.let {
                    gl.deleteRenderBuffer(it)
                }
                stencilBufferHandle = null
            }
            if (hasPackedDepthStencil) {
                depthStencilPackedBufferHandle?.let {
                    gl.deleteRenderBuffer(it)
                }
                depthStencilPackedBufferHandle = null
            }

            depthStencilPackedBufferHandle = gl.createRenderBuffer().also {
                gl.bindRenderBuffer(it)
                gl.renderBufferStorage(RenderBufferInternalFormat.DEPTH24_STENCIL8_OES, width, height)
                gl.bindDefaultRenderBuffer()
                gl.frameBufferRenderBuffer(FrameBufferRenderBufferAttachment.DEPTH_ATTACHMENT, it)
                gl.frameBufferRenderBuffer(FrameBufferRenderBufferAttachment.STENCIL_ATTACHMENT, it)
            }
            hasPackedDepthStencil = true
            result = gl.checkFrameBufferStatus()
        }

        gl.bindDefaultFrameBuffer()

        if (result != FrameBufferStatus.FRAMEBUFFER_COMPLETE) {
            dispose()

            when (result) {
                FrameBufferStatus.FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> throw IllegalStateException("Frame buffer couldn't be constructed: incomplete attachment")
                FrameBufferStatus.FRAMEBUFFER_INCOMPLETE_DIMENSIONS -> throw IllegalStateException("Frame buffer couldn't be constructed: incomplete dimensions")
                FrameBufferStatus.FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> throw IllegalStateException("Frame buffer couldn't be constructed: missing attachment")
                FrameBufferStatus.FRAMEBUFFER_UNSUPPORTED -> throw IllegalStateException("Frame buffer couldn't be constructed: unsupported combination of formats")
                else -> throw IllegalStateException("Frame buffer couldn't be constructed: unknown error ${result.glFlag}")
            }
        }
        isPrepared = true
    }

    /**
     * Binds the frame buffer and sets the viewport to the [width] and [height].
     */
    fun begin() {
        val fboHandle = fboHandle
        check(isPrepared && fboHandle != null) { "The framebuffer has not been prepared yet! Ensure you called prepare() sometime before you call begin()" }

        gl.bindFrameBuffer(fboHandle)
        gl.viewport(0, 0, width, height)
    }

    /**
     * Binds the default framebuffer and sets the [GL.viewport] with the given position and size.
     * @param x the viewport x
     * @param y the viewport y
     * @param width the viewport width
     * @param height the viewport height
     */
    fun end(
        x: Int = 0,
        y: Int = 0,
        width: Int = context.graphics.backBufferWidth,
        height: Int = context.graphics.backBufferHeight,
    ) {
        val fboHandle = fboHandle
        check(isPrepared && fboHandle != null) { "The framebuffer has not been prepared yet! Ensure you called prepare() sometime before you call end()" }

        gl.bindDefaultFrameBuffer()
        gl.viewport(x, y, width, height)
    }

    override fun dispose() {
        _textures.forEach {
            it.dispose()
        }

        if (hasDepth) {
            depthBufferHandle?.let {
                gl.deleteRenderBuffer(it)
            }
            depthBufferHandle = null
        }
        if (hasStencil) {
            stencilBufferHandle?.let {
                gl.deleteRenderBuffer(it)
            }
            stencilBufferHandle = null
        }
        if (hasPackedDepthStencil) {
            depthStencilPackedBufferHandle?.let {
                gl.deleteRenderBuffer(it)
            }
            depthStencilPackedBufferHandle = null
        }
        fboHandle?.let { gl.deleteFrameBuffer(it) }
    }
}

@OptIn(ExperimentalContracts::class)
inline fun FrameBuffer.use(action: (FrameBuffer) -> Unit) {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    begin()
    action(this)
    end()
}