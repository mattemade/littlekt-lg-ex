package net.mattemade.bossrush.scene

import com.littlekt.graphics.g2d.Batch
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.radians
import com.littlekt.math.isFuzzyZero
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import kotlin.math.sqrt
import kotlin.time.Duration

class Arena(var rotation: Float = 0f, private val assets: Assets) {

    private val textureSize = Vec2f(assets.texture.arena.width.toFloat(), assets.texture.arena.height.toFloat())
    private val startPosition = Vec2f(-assets.texture.arena.width / 2f, -assets.texture.arena.height / 2f)
    private val tempVec2f = MutableVec2f()

    var angularVelocity: Float = 0f

    fun update(dt: Duration) {
        rotation += dt.seconds * angularVelocity
        if (angularVelocity > 0f) {
            angularVelocity = maxOf(0f, angularVelocity - dt.seconds / 8f)
        } else {
            angularVelocity = minOf(0f, angularVelocity + dt.seconds / 8f)
        }
    }

    fun adjustVelocity(previousPosition: Vec2f, position: Vec2f, scale: Float) {
        val angle = previousPosition.angleTo(position).radians
        if (!angle.isFuzzyZero()) {
            val distanceBasedFadeOut = sqrt(previousPosition.length() * position.length()) / 10f
            // TODO: account for dt!!! otherwise it will break on different FPS
            if (angle > 0f) {
                angularVelocity = minOf(1f, angularVelocity + angle * scale * distanceBasedFadeOut)
            } else {
                angularVelocity = maxOf(-1f, angularVelocity + angle * scale * distanceBasedFadeOut)
            }
        }
    }

    fun render(batch: Batch) {
        val angle = rotation.radians
        tempVec2f.set(startPosition).rotate(angle)
        batch.draw(assets.texture.arena, x = tempVec2f.x, y = tempVec2f.y, width = textureSize.x, height = textureSize.y, rotation = angle)
    }
}