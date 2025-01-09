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
import net.mattemade.bossrush.objects.TemporaryDepthRenderableObject
import net.mattemade.bossrush.objects.TestBoss
import net.mattemade.bossrush.objects.Trap
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

    private val gameObjects = mutableListOf<TemporaryDepthRenderableObject>()

    private fun <T : TemporaryDepthRenderableObject> T.save(): T {
        gameObjects.add(this)
        return this
    }

    private val player by lazy { Player(context, input, assets, ::placingTrap).save() }
    private val arena by lazy { Arena(0f, assets) }
    private var shapeRenderer: ShapeRenderer? = null


    private val testBoss by lazy {
        TestBoss(player, assets) { it.save() }.save()
    }

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
        tempVec2f.set(player.position).add(testBoss.position).scale(0.5f)
        camera.position.set(tempVec2f.x, tempVec2f.y, 0f)
        camera.update()
        arena.adjustVelocity(player.previousPosition, player.position, 0.05f)
        arena.update(dt)

        gameObjects.fastIterateRemove {
            var isAlive = it.update(dt)
            if (isAlive) {
                it.displace(getDisplacementAt(position = it.position, dt = dt))
                when (it) {
                    is Trap -> {
                        if (it.activatedTimeToLive < 0f) {
                            if (it.position.distance(player.position) < 10f) {
                                player.trapped()
                                it.activate()
                            }
                            if (it.position.distance(testBoss.position) < 20f) {
                                testBoss.trapped()
                                it.activate()
                            }
                        }
                    }
                    is Projectile -> {
                        if (it.timeToLive > 0f) {
                            // collision check
                            if (player.position.distance(it.position) < 8f) {
                                isAlive = false
                                // TODO: hit player
                            } /* else if (player.racketPosition.distance(it.position) < 10f) {
                                isAlive = false
                            }*/
                        }
                    }
                }
            }
            !isAlive
        }
    }

    private fun render(batch: Batch) {
        (shapeRenderer ?: ShapeRenderer(batch).also { shapeRenderer = it }).let { shapeRenderer ->
            arena.render(batch)
            gameObjects.sort()
            gameObjects.fastForEach {
                it.renderShadow(shapeRenderer)
            }
            gameObjects.fastForEach {
                it.render(batch, shapeRenderer)
            }
        }
    }

    private fun placingTrap(seconds: Float, place: Boolean) {
        if (place) {
            Trap(MutableVec2f(player.racketPosition), assets).save()
        }
    }

    fun updateAndRender(dt: Duration) {
        renderer.render(dt)
    }

    fun resize(width: Int, height: Int) {
        renderer.resize(width, height, 320f, 240f)
    }
}