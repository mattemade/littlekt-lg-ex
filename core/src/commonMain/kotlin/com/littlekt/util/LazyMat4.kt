package com.littlekt.util

import com.littlekt.math.Mat4

/**
 * @author Colton Daily
 * @date 11/27/2021
 */
class LazyMat4(val update: (Mat4) -> Unit) {
    private val mat = Mat4()

    var isDirty = true
    fun get(): Mat4 {
        if (isDirty) {
            update(mat)
            isDirty = false
        }
        return mat
    }

    fun clear() {
        isDirty = false
        mat.setToIdentity()
    }
}