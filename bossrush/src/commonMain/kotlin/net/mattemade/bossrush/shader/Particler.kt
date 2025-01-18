package net.mattemade.bossrush.shader

import com.littlekt.Context
import com.littlekt.graphics.GL
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.gl.BlendFactor
import com.littlekt.graphics.gl.State
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.math.MutableVec2f
import com.littlekt.util.milliseconds
import net.mattemade.bossrush.shader.representation.BoundableBuffer
import kotlin.time.Duration

class Particler(
    private val context: Context,
    private val shader: ShaderProgram<ParticleVertexShader, ParticleFragmentShader>,
    val position: MutableVec2f,
    private val instances: Int,
    private val lifeTime: Float,
    private val size: Float,
    private val interpolation: Int = 0,
    private val fillData: (
        index: Int,
        startColor: FloatArray,
        endColor: FloatArray,
        startPosition: FloatArray,
        endPosition: FloatArray,
        activeBetween: FloatArray
    ) -> Unit,
    private val die: (Particler) -> Unit
) {

    //override val depth: Float = y

    private val verticies = BoundableBuffer(
        context, floatArrayOf(
            0f, 0f,
            0f, size,
            size, 0f,
            size, size,
        ), 2, instanced = false
    )


    private val vertexPosition = shader.getAttrib("vertexPosition")
    private val startPositionLocation = shader.getAttrib("startPosition")
    private val endPositionLocation = shader.getAttrib("endPosition")
    private val startColorLocation = shader.getAttrib("startColor")
    private val endColorLocation = shader.getAttrib("endColor")
    private val activeBetweenLocation = shader.getAttrib("activeBetween")

    private val startColor: BoundableBuffer
    private val endColor: BoundableBuffer
    private val startPosition: BoundableBuffer
    private val endPosition: BoundableBuffer
    private val activeBetween: BoundableBuffer

    private val mutableStartColorArray = FloatArray(4)
    private val mutableEndColorArray = FloatArray(4)
    private val mutableStartPositionArray = FloatArray(2)
    private val mutableEndPositionArray = FloatArray(2)
    private val mutableActiveBetweenArray = FloatArray(2)

    init {
        val startColorArray = FloatArray(instances * 4)
        val endColorArray = FloatArray(instances * 4)
        val startPositionArray = FloatArray(instances * 2)
        val endPositionArray = FloatArray(instances * 2)
        val activeBetweenArray = FloatArray(instances * 2)
        for (index in 0 until instances) {
            fillData(
                index,
                mutableStartColorArray,
                mutableEndColorArray,
                mutableStartPositionArray,
                mutableEndPositionArray,
                mutableActiveBetweenArray
            )
            mutableStartColorArray.copyInto(startColorArray, index * 4)
            mutableEndColorArray.copyInto(endColorArray, index * 4)
            mutableStartPositionArray.copyInto(startPositionArray, index * 2)
            mutableEndPositionArray.copyInto(endPositionArray, index * 2)
            mutableActiveBetweenArray.copyInto(activeBetweenArray, index * 2)
        }

        startColor = BoundableBuffer(context, startColorArray, 4, instanced = true)
        endColor = BoundableBuffer(context, endColorArray, 4, instanced = true)
        startPosition = BoundableBuffer(context, startPositionArray, 2, instanced = true)
        endPosition = BoundableBuffer(context, endPositionArray, 2, instanced = true)
        activeBetween = BoundableBuffer(context, activeBetweenArray, 2, instanced = true)
    }

    var time = 0f

    fun render(batch: Batch) {
        val originalMatrix = batch.projectionMatrix
        batch.end()

        shader.bind()

        verticies.bind(vertexPosition)
        startPosition.bind(startPositionLocation)
        endPosition.bind(endPositionLocation)
        startColor.bind(startColorLocation)
        endColor.bind(endColorLocation)
        activeBetween.bind(activeBetweenLocation)
        shader.uProjTrans?.apply(shader, originalMatrix)
        shader.vertexShader.uTime.apply(shader, time)
        shader.vertexShader.uInterpolation.apply(shader, interpolation)
        shader.vertexShader.uOffsetX.apply(shader, position.x * 2f)
        shader.vertexShader.uOffsetY.apply(shader, position.y * 2f)

        context.gl.enable(State.BLEND)
        context.gl.blendFuncSeparate(
            BlendFactor.SRC_ALPHA,
            BlendFactor.ONE_MINUS_SRC_ALPHA,
            BlendFactor.SRC_ALPHA,
            BlendFactor.ONE_MINUS_SRC_ALPHA
        )
        context.gl.drawArraysInstanced(
            GL.TRIANGLE_STRIP,
            0,             // offset
            verticies.elements,   // num vertices per instance
            startPosition.elements
        )
        context.gl.disable(State.BLEND)

        batch.begin(originalMatrix)
    }

    fun update(dt: Duration): Boolean {
        time += dt.milliseconds
        if (time > lifeTime) {
            die(this)
        }
        return time > lifeTime
    }
}
