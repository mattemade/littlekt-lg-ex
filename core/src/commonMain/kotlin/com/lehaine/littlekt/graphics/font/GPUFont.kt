package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.fragment.GlyphFragmentShader
import com.lehaine.littlekt.graphics.shader.fragment.SimpleColorFragmentShader
import com.lehaine.littlekt.graphics.shader.fragment.TextFragmentShader
import com.lehaine.littlekt.graphics.shader.vertex.DefaultVertexShader
import com.lehaine.littlekt.graphics.shader.vertex.GlyphVertexShader
import com.lehaine.littlekt.graphics.shader.vertex.TextVertexShader
import com.lehaine.littlekt.math.*
import com.lehaine.littlekt.util.datastructure.Pool

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
class GPUFont(font: TtfFont) : Preparable {
    private lateinit var glyphCompiler: GlyphCompiler
    private val glyphs = font.glyphs
    private val pool = Pool(10) { GPUGlyph() }
    private val instances = mutableListOf<GPUGlyph>()

    private val glyphVertexShader = GlyphVertexShader()
    private val glyphFragmentShader = GlyphFragmentShader()
    private lateinit var glyphShader: ShaderProgram

    private lateinit var defaultShader: ShaderProgram

    private val textVertexShader = TextVertexShader()
    private val textFragmentShader = TextFragmentShader()
    private lateinit var textShader: ShaderProgram
    private lateinit var mesh: Mesh
    private lateinit var gl: GL

    private var isPrepared = false
    private val fbo = FrameBuffer(960, 540)
    val ascender = font.ascender
    val descender = font.descender
    val unitsPerEm = font.unitsPerEm

    override val prepared: Boolean
        get() = isPrepared

    override fun prepare(application: Application) {
        gl = application.gl
        glyphShader = ShaderProgram(application.gl, glyphVertexShader, glyphFragmentShader)
        textShader = ShaderProgram(application.gl, textVertexShader, textFragmentShader)
        defaultShader = ShaderProgram(application.gl, DefaultVertexShader(), SimpleColorFragmentShader())
        mesh = colorMesh(application.gl) {
            maxVertices = 5000
        }.also {
            it.setIndicesAsTriangle()
        }
        glyphCompiler = GlyphCompiler(mesh)
        fbo.prepare(application)
        isPrepared = true
    }


    fun text(text: String, x: Float, y: Float) {
        check(isPrepared) { "GPUFont has not been prepared yet! Please call prepare() before using!" }
        compileGlyphs(text, x, y)
    }

    private val temp = Mat4()
    private val redBits = Color.RED.toFloatBits()

    fun flush(batch: SpriteBatch, viewProjection: Mat4) {
        //  fbo.begin()
//        gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
//        gl.blendFunc(BlendFactor.ONE, BlendFactor.ONE)
//        gl.enable(State.SCISSOR_TEST)
//        glyphShader.bind()
//        JITTER_PATTERN.forEachIndexed { idx, pattern ->
//            temp.set(viewProjection)
//            temp.translate(pattern.x, pattern.y, 0f)
//            if (idx % 2 == 0) {
//                glyphFragmentShader.uColor.apply(
//                    glyphShader,
//                    if (idx == 0) 1f else 0f,
//                    if (idx == 2) 1f else 0f,
//                    if (idx == 4) 1f else 0f,
//                    0f
//                )
//            }
//            glyphVertexShader.uProjTrans.apply(glyphShader, temp)
//            mesh.render(glyphShader)
//        }
        //   fbo.end()
//        gl.blendFunc(BlendFactor.ZERO, BlendFactor.SRC_COLOR)
//        gl.disable(State.SCISSOR_TEST)
//        batch.shader = textShader
//        batch.use(viewProjection) {
//            textFragmentShader.uTex.apply(textShader, fbo.colorBufferTexture.glTexture!!)
//            textFragmentShader.uColor.apply(textShader, Color.CLEAR)
//            it.draw(fbo.colorBufferTexture, -1f, -1f, flipY = true)
//        }
//
//        batch.shader = batch.defaultShader
//        fbo.begin()
        //      gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        defaultShader.bind()
        defaultShader.uProjTrans?.apply(defaultShader, viewProjection)
        val count = idx / 12 * 6
        mesh.render(defaultShader, count = count)
//        fbo.end()
//        batch.use(viewProjection) {
//            it.draw(fbo.colorBufferTexture, -300f, -1f, flipY = true)
//        }
        idx = 0
        pool.free(instances)
        instances.clear()
    }

    private val bits = Color.WHITE.toFloatBits()
    private var idx = 0
    private fun compileGlyphs(text: String, x: Float, y: Float) {
        var tx = x
        val scale = 1f / unitsPerEm * 100f
        val size = 50f * scale
        text.forEach { char ->
            val code = char.code
            val glyph = glyphs[code] ?: error("Unable to find glyph for '$char'!")
            val gpuGlyph = pool.alloc().also {
                it.glyph = glyph
                it.offset.set(tx, y)
                instances += it
            }

            glyph.points.forEach {
                mesh.setVertex {
                    this.x = it.x.toFloat() * scale + tx
                    this.y = it.y.toFloat() * scale + y
                    this.colorPacked = bits
                }

                mesh.setVertex {
                    this.x = it.x.toFloat() * scale + tx + size
                    this.y = it.y.toFloat() * scale + y
                    this.colorPacked = bits
                }
                mesh.setVertex {
                    this.x = it.x.toFloat() * scale + tx + size
                    this.y = it.y.toFloat() * scale + y + size
                    this.colorPacked = bits
                }
                mesh.setVertex {
                    this.x = it.x.toFloat() * scale + tx
                    this.y = it.y.toFloat() * scale + y + size
                    this.colorPacked = bits
                }
                idx += 12
            }

            tx += glyph.advanceWidth * scale

        }
//        text.forEach { char ->
//            val code = char.code
//            val glyph = glyphs[code] ?: error("Unable to find glyph for '$char'!")
//            val gpuGlyph = pool.alloc().also {
//                it.glyph = glyph
//                it.offset.set(tx, y)
//                tx += glyph.advanceWidth * scale
//                instances += it
//            }
//            if (char != ' ') {
//                glyphCompiler.begin(gpuGlyph)
//                gpuGlyph.glyph?.path?.commands?.forEach { cmd ->
//                    when (cmd.type) {
//                        GlyphPath.CommandType.MOVE_TO -> glyphCompiler.moveTo(cmd.x * scale, cmd.y * scale)
//                        GlyphPath.CommandType.LINE_TO -> glyphCompiler.lineTo(cmd.x * scale, cmd.y * scale)
//                        GlyphPath.CommandType.QUADRATIC_CURVE_TO -> glyphCompiler.curveTo(
//                            cmd.x1 * scale,
//                            cmd.y1 * scale,
//                            cmd.x * scale,
//                            cmd.y * scale
//                        )
//                        GlyphPath.CommandType.CLOSE -> glyphCompiler.close()
//                        else -> {
//                            // do nothing with bezier curves - only want the quadratic curves
//                        }
//                    }
//                }
//                glyphCompiler.end()
//            }
//        }
    }

    companion object {
        private val JITTER_PATTERN = listOf(
            Vec2f(-1f / 12f, -5f / 12f),
            Vec2f(1f / 12f, 1f / 12f),
            Vec2f(3f / 12f, -1f / 12f),
            Vec2f(5f / 12f, 5f / 12f),
            Vec2f(7f / 12f, -3f / 12f),
            Vec2f(0f / 12f, 3f / 12f)
        )
    }
}

internal data class GPUGlyph(
    var glyph: Glyph? = null,
    var bounds: Rect = Rect(),
    val offset: MutableVec2f = MutableVec2f(0f)
)

enum class TriangleType {
    SOLID,
    QUADRATIC_CURVE
}

internal class GlyphCompiler(val mesh: Mesh) {
    private val vertices = mutableListOf<Float>()
    private var firstX = 0f
    private var firstY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var contourCount = 0
    private var glyph: GPUGlyph? = null
    private var rectBuilder = RectBuilder()
    private var color: Color = Color.WHITE.withAlpha(0.5f)
    private var colorBits = color.toFloatBits()

    fun begin(glyph: GPUGlyph, color: Color = Color.WHITE) {
        this.glyph = glyph
        if (this.color != color) {
            this.color = color
            colorBits = color.toFloatBits()
        }
        rectBuilder.reset()
        vertices.clear()
    }

    fun moveTo(x: Float, y: Float) {
        firstX = x
        currentX = x
        firstY = y
        currentY = y
        contourCount = 0
    }

    fun lineTo(x: Float, y: Float) {
        if (++contourCount >= 2) {
            appendTriangle(firstX, firstY, currentX, currentY, x, y, TriangleType.SOLID)
        }
        currentX = x
        currentY = y
    }

    fun curveTo(cx: Float, cy: Float, x: Float, y: Float) {
        if (++contourCount >= 2) {
            appendTriangle(firstX, firstY, currentX, currentX, x, y, TriangleType.SOLID)
        }
        appendTriangle(currentX, currentX, cx, cy, x, y, TriangleType.QUADRATIC_CURVE)
        currentX = x
        currentY = y
    }

    fun close() {
        currentX = firstX
        currentY = firstY
        contourCount = 0
    }

    fun end() {
        glyph?.bounds = rectBuilder.build()
    }

    fun appendTriangle(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
        triangleType: TriangleType
    ) {
        when (triangleType) {
            TriangleType.SOLID -> {
                appendVertex(ax, ay, 0f, 1f)
                appendVertex(bx, by, 0f, 1f)
                appendVertex(cx, cy, 0f, 1f)
            }
            TriangleType.QUADRATIC_CURVE -> {
                appendVertex(ax, ay, 0f, 0f)
                appendVertex(bx, by, 0.5f, 0f)
                appendVertex(cx, cy, 1f, 1f)
            }
        }
    }

    fun appendVertex(x: Float, y: Float, u: Float, v: Float) {
        rectBuilder.include(x, y)
        mesh.setVertex {
            this.x = x
            this.y = y
            this.u = u
            this.v = v
            this.colorPacked = colorBits
        }
    }
}