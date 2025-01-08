package net.mattemade.bossrush.scene

import com.littlekt.Context
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.input.InputMapController
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.radians
import com.littlekt.util.fastForEach
import com.littlekt.util.fastIterateRemove
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.input.GameInput
import net.mattemade.bossrush.objects.Projectile
import net.mattemade.bossrush.objects.ProjectileSpawner
import net.mattemade.bossrush.player.Player
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.DirectRender
import kotlin.time.Duration

class Fight(
    private val context: Context,
    private val input: InputMapController<GameInput>,
    private val assets: Assets,
) : Releasing by Self() {

    private val player by lazy { Player(context, input, assets) }
    private val arena by lazy { Arena(0f, assets) }
    private var shapeRenderer: ShapeRenderer? = null


    private val projectileSpawner by lazy {
        ProjectileSpawner(player) {
            //projectiles += it
        }
    }
    private val projectiles = mutableListOf<Projectile>()

    private var renderer = DirectRender(
        context,
        1920,
        1080,
        updateCall = { dt, camera ->
            update(camera, dt)
        },
        renderCall = { dt, batch ->
            render(batch)
        }
    )

    private val tempVec2f = MutableVec2f()

    private fun getDisplacementAt(position: Vec2f, dt: Duration): Vec2f {
        // TODO: apply active turn-tables too
        tempVec2f.set(position).rotate((arena.angularVelocity * dt.seconds).radians).minusAssign(position)
        return tempVec2f
    }

    private fun update(camera: Camera, dt: Duration) {
        camera.position.set(player.position.x / 2f, player.position.y / 2f, 0f)
        camera.update()
        player.update(dt)
        arena.adjustVelocity(player.previousPosition, player.position, 0.05f)
        arena.update(dt)
        projectileSpawner.update(dt)

        player.displace(getDisplacementAt(player.position, dt))

        projectiles.fastIterateRemove {
            it.update(dt)
            var remove = it.timeToLive <= 0f
            if (it.timeToLive > 0f) {
                // collision check
                if (player.position.distance(it.position) < 20f) {
                    remove = true
                } else if (player.racketPosition.distance(it.position) < 10f) {
                    remove = true
                }
            }
            remove
        }

    }

    private fun render(batch: Batch) {
        (shapeRenderer ?: ShapeRenderer(batch).also { shapeRenderer = it }).let { shapeRenderer ->
            arena.render(batch)
            projectiles.fastForEach {
                if (it.position.y < player.position.y) {
                    it.render(shapeRenderer)
                }
            }
            //projectileSpawner.render(shapeRenderer)
            player.render(batch, shapeRenderer)
            projectiles.fastForEach {
                if (it.position.y >= player.position.y) {
                    it.render(shapeRenderer)
                }
            }
        }
    }


    fun updateAndRender(dt: Duration) {
        renderer.render(dt)
    }

    fun resize(width: Int, height: Int) {
        renderer.resize(width, height, 320f, 240f)
    }
}