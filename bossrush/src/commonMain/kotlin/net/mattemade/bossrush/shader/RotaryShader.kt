package net.mattemade.bossrush.shader

import com.littlekt.graphics.shader.FragmentShaderModel
import com.littlekt.graphics.shader.ShaderParameter
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.graphics.shader.VertexShaderModel
import com.littlekt.graphics.shader.shaders.DefaultVertexShader

typealias RotaryShader = ShaderProgram<DefaultVertexShader, RotaryFragmentShader>

fun createRotaryShader(fragment: String): RotaryShader =
    ShaderProgram(
        DefaultVertexShader(),
        RotaryFragmentShader(fragment)
    )

class RotaryFragmentShader(private val sourceText: String) : FragmentShaderModel() {
    val uTime = ShaderParameter.UniformFloat("u_time")
    val uAlpha = ShaderParameter.UniformFloat("u_alpha")
    val uScale = ShaderParameter.UniformFloat("u_scale")

    override val parameters: LinkedHashSet<ShaderParameter> = linkedSetOf(uTime, uAlpha, uScale)

    override var source: String = sourceText
}