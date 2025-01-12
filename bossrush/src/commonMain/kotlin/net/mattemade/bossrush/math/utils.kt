package net.mattemade.bossrush.math

import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.Vec2f
import com.littlekt.math.clamp
import com.littlekt.math.geom.radians
import net.mattemade.bossrush.NO_ROTATION
import kotlin.math.abs


fun minByAbs(a: Float, b: Float): Float = if (abs(a) < abs(b)) a else b

fun minimalRotation(angleFrom: Float, angleTo: Float): Float {
    val diff = angleTo - angleFrom
    return minByAbs(minByAbs(diff, diff + PI2_F), diff - PI2_F)
}

fun MutableVec2f.rotateTowards(target: Vec2f, limit: Float = Float.MAX_VALUE - 10f) {
    val originalRotation = this.angleTo(NO_ROTATION).radians
    val newRotation = target.angleTo(NO_ROTATION).radians
    val difference = minimalRotation(originalRotation, newRotation)
    /*val newRotationClockwise = newRotationCounter + PI2_F

    val counterDifference = newRotationCounter - originalRotation
    val clockwiseDifference = newRotationClockwise - originalRotation

    val newRotation = if (kotlin.math.abs(counterDifference) < kotlin.math.abs(clockwiseDifference)) {
        newRotationCounter
    } else {
        newRotationClockwise
    }*/
    rotate(difference.clamp(-limit, limit).radians)
}
