package net.mattemade.bossrush.objects

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import net.mattemade.bossrush.Assets

class Column(override val position: MutableVec2f, private val assets: Assets): TemporaryDepthRenderableObject {

    override fun displace(displacement: Vec2f) {
        position.add(displacement)
    }

    override val solidRadius: Float
        get() = 16f

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        batch.draw(assets.texture.column, x = position.x - 16f, y = position.y - 56f, width = 32f, height = 64f)
    }
}