package net.mattemade.bossrush.objects

import com.littlekt.graphics.g2d.Batch
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import kotlin.time.Duration

class Trap(val position: MutableVec2f, private val assets: Assets) {

    private val texture = assets.texture.trap
    private val textureActivated = assets.texture.trapActivated
    var activatedTimeToLive = -1f

    fun displace(displacement: Vec2f) {
        position.add(displacement)
    }

    fun update(dt: Duration) {
        if (activatedTimeToLive > 0f) {
            activatedTimeToLive = maxOf(0f, activatedTimeToLive - dt.seconds)
        }
    }

    fun render(batch: Batch) {
        batch.draw(
            if (activatedTimeToLive > 0f) textureActivated else texture,
            x = position.x - 16f,
            y = position.y - 24f,
            width = 32f,
            height = 32f,
        )
    }

    fun activate() {
        activatedTimeToLive = 1f
    }
}
