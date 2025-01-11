package net.mattemade.bossrush.math

import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.Vec2f
import com.littlekt.math.clamp
import com.littlekt.math.geom.radians
import net.mattemade.bossrush.NO_ROTATION

fun MutableVec2f.rotateTowards(target: Vec2f, limit: Float = Float.MAX_VALUE - 10f) {
    val originalRotation = this.angleTo(NO_ROTATION).radians
    val newRotationCounter = target.angleTo(NO_ROTATION).radians
    val newRotationClockwise = newRotationCounter + PI2_F

    val counterDifference = newRotationCounter - originalRotation
    val clockwiseDifference = newRotationClockwise - originalRotation

    val newRotation = if (kotlin.math.abs(counterDifference) < kotlin.math.abs(clockwiseDifference)) {
        newRotationCounter
    } else {
        newRotationClockwise
    }
    rotate((newRotation - originalRotation).clamp(-limit, limit).radians)
}
