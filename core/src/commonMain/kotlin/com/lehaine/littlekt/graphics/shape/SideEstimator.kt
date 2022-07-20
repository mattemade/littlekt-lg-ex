package com.lehaine.littlekt.graphics.shape

import com.lehaine.littlekt.math.PI2_F
import com.lehaine.littlekt.math.clamp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * @author Colton Daily
 * @date 7/18/2022
 */
interface SideEstimator {

    fun estimateSidesRequired(pixelSize: Float, rx: Float, ry: Float): Int {
        val circumference = (PI2_F * sqrt((rx * rx + ry * ry) / 2f))
        var sides = (circumference / (16 * pixelSize)).toInt()
        val a = min(rx, ry)
        val b = max(rx, ry)
        val eccentricity = sqrt(1 - ((a * a) / (b * b)))
        sides += ((sides * eccentricity) / 16).toInt()
        return sides.clamp(20, 4000)
    }
}