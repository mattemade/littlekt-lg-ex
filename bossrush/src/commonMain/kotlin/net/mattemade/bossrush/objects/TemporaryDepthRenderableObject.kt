package net.mattemade.bossrush.objects

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.Vec2f

interface TemporaryDepthRenderableObject : Comparable<TemporaryDepthRenderableObject> {

    val position: Vec2f

    fun update(dt: kotlin.time.Duration): Boolean = true // true if still alive

    fun displace(displacement: Vec2f) {}

    fun render(batch: Batch, shapeRenderer: ShapeRenderer) {}

    fun renderShadow(shapeRenderer: ShapeRenderer) {}

    val solidRadius: Float?
        get() = null

    override fun compareTo(other: TemporaryDepthRenderableObject): Int =
        position.y.compareTo(other.position.y)
}