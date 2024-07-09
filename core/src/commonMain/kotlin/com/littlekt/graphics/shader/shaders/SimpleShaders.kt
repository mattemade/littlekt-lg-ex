package com.littlekt.graphics.shader.shaders

import com.littlekt.graphics.shader.FragmentShaderModel
import com.littlekt.graphics.shader.VertexShaderModel
import com.littlekt.graphics.shader.generator.Precision
import com.littlekt.graphics.shader.generator.type.mat.Mat4
import com.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class SimpleColorVertexShader : VertexShaderModel() {
    private val u_projTrans by uniform(::Mat4)
    private val a_position by attribute(::Vec4)
    private val a_color by attribute(::Vec4)
    private var v_color by varying(::Vec4)

    init {
        v_color = a_color
        gl_Position = u_projTrans * a_position
    }
}

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class SimpleColorFragmentShader : FragmentShaderModel() {
    private val v_color by varying(::Vec4, Precision.LOW)

    init {
        gl_FragColor = v_color
    }
}
