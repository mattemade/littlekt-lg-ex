package com.littlekt.graphics.gl

import org.khronos.webgl.WebGLBuffer

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
actual class GlBuffer(val delegate: WebGLBuffer) {
    val bufferId = nextBufferId++

    companion object {
        private var nextBufferId = 1L
    }
}