package com.lehaine.littlekt.graphics.shader.fragment

import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.Sampler2D
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 11/29/2021
 */
class TextFragmentShader : FragmentShaderModel() {
    val uColor get() = parameters[0] as ShaderParameter.UniformVec4
    val uTex get() = parameters[1] as ShaderParameter.UniformSample2D

    private val u_color by uniform(::Vec4)
    private val u_texture by uniform(::Sampler2D)
    private val v_color by varying(::Vec4)
    private val v_texCoords by varying(::Vec2)

    init {
        // Get samples for -2/3 and -1/3
        val valueL by vec2(texture2D(u_texture, vec2Lit(v_texCoords.x + dFdx(v_texCoords.x), v_texCoords.y)).yz * 255f)
        val lowerL by vec2(mod(valueL, 16f.lit))
        val upperL by vec2((valueL - lowerL) / 16f.lit)
        val alphaL by vec2(min(abs(upperL - lowerL), 2f.lit))

        // Get samples for 0, +1/3, and +2/3
        val valueR by vec3(texture2D(u_texture, v_texCoords).xyz * 255f.lit)
        val lowerR by vec3(mod(valueR, 16f.lit))
        val upperR by vec3((valueR - lowerR) / 16f.lit)
        val alphaR by vec3(min(abs(upperR - lowerR), 2f.lit))

        // Average the energy over the pixels on either side
        val rgba by vec4(
            x = (alphaR.x + alphaR.y + alphaR.z) / 6f,
            y = (alphaL.y + alphaR.x + alphaR.y) / 6f,
            z = (alphaL.x + alphaL.y + alphaR.x) / 6f,
            w = 0f
        )

        // Optionally scale by a color
        gl_FragColor = ternary(u_color.a eq 0f.lit, left = 1f.lit - rgba, right = u_color * rgba)
    }
}