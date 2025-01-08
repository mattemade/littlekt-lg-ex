package net.mattemade.bossrush.objects

import com.littlekt.graphics.g2d.Batch
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import net.mattemade.bossrush.Assets

class Trap(val position: MutableVec2f, private val assets: Assets) {

    private val texture = assets.texture.trap

    fun displace(displacement: Vec2f) {
        position.add(displacement)
    }

    fun render(batch: Batch) {
        batch.draw(
            texture,
            x = position.x - 16f,
            y = position.y - 30f,
            width = 32f,
            height = 32f,
        )
    }
}