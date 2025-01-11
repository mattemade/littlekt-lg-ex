package net.mattemade.bossrush.scene

import com.littlekt.Context
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.input.InputMapController
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI_F
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.radians
import com.littlekt.util.fastForEach
import com.littlekt.util.fastIterateRemove
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.input.GameInput
import net.mattemade.bossrush.math.rotateTowards
import net.mattemade.bossrush.objects.Column
import net.mattemade.bossrush.objects.Projectile
import net.mattemade.bossrush.objects.Swing
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
    private val solidObjects = mutableListOf<TemporaryDepthRenderableObject>()

    private fun <T : TemporaryDepthRenderableObject> T.save(): T {
        gameObjects.add(this)
        return this
    }

    private fun <T : TemporaryDepthRenderableObject> T.solid(): T {
        gameObjects.add(this)
        solidObjects.add(this)
        return this
    }

    private val player by lazy { Player(context, input, assets, ::placingTrap, ::swing).solid() }
    private val arena by lazy {
        Column(MutableVec2f(-50f, 0f), assets).solid()
        //Column(MutableVec2f(50f, 0f), assets).solid()
        Arena(0f, assets)
    }
    private var shapeRenderer: ShapeRenderer? = null


    private val testBoss by lazy {
        TestBoss(player, assets) { it.save() }.solid()
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

        gameObjects.fastIterateRemove { obj -> !obj.update(dt).also { if (!it) solidObjects.remove(obj) } }
        arena.adjustVelocity(player.previousPosition, player.position, 0.05f)
        arena.update(dt)

        solidObjects.fastForEach {
            if (it != player) {
                it.solidRadius?.let { solidRadius ->
                    if (player.position.distance(it.position) < player.solidRadius + solidRadius) {
                        // push player away
                        tempVec2f.set(player.position).subtract(it.position).setLength(player.solidRadius + solidRadius)
                        player.position.set(it.position).add(tempVec2f)
                    }
                }
            }
            // TODO: the same for boss
        }
        gameObjects.fastIterateRemove {
            it.displace(getDisplacementAt(position = it.position, dt = dt))
            when (it) {
                is Trap -> {
                    if (it.activatedTimeToLive < 0f) {
                        if (it.position.distance(player.position) < player.solidRadius + it.solidRadius) {
                            player.trapped()
                            it.activate()
                        }
                        if (it.position.distance(testBoss.position) < testBoss.solidRadius + it.solidRadius * 3f) {
                            testBoss.trapped()
                            it.activate()
                        }
                    }
                    false // do not remove trap here
                }

                is Projectile -> {
                    if (it.timeToLive > 0f) {
                        // collision check
                        var collide = false
                        solidObjects.fastForEach { solid ->
                            if (it.canDamageBoss || solid !== testBoss) { // do not collide with boss if we can't damage
                                solid.solidRadius?.let { solidRadius ->
                                    if (solid.position.distance(it.position) < solidRadius) {
                                        collide = true
                                        if (solid === player) {
                                            player.damaged()
                                        } else if (solid === testBoss) {
                                            testBoss.damaged()
                                        }
                                    }
                                }
                            }
                        }
                        collide
                    } else {
                        false
                    }
                }

                else -> false
            }
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

    private fun swing(angle: Float, clockwise: Boolean, powerful: Boolean) {
        val swing = Swing(player.position, angle, clockwise, assets, powerful).save()
        gameObjects.fastForEach {
            if (it is Projectile) {
                if (swing.hitFrontPosition.distance(it.position) < swing.hitFrontRadius &&
                    swing.hitBackPosition.distance(it.position) > swing.hitBackRadius
                ) {
                    it.canDamageBoss = true
                    if (powerful) {
                        tempVec2f.set(it.position).subtract(player.position).rotate((if (clockwise) -PI_F *0.4f else PI_F *0.4f).radians)
                        it.direction.rotateTowards(tempVec2f)
                        //var movingDistance = it.position.distance(testBoss.position)
                        it.target = {
                            // TODO: think about restricting spinning the opposit direction after passing through the boss
                            /*val currentDistance = it.position.distance(testBoss.position)
                            if (currentDistance > movingDistance) {
                                it.target = null
                            } else {
                                movingDistance = currentDistance
                            }*/
                            testBoss.position
                        }
                    } else {
                        tempVec2f.set(it.position).subtract(player.position)
                        it.direction.rotateTowards(tempVec2f)
                    }
                }
            } else if (it is TestBoss) {
                if (swing.hitFrontPosition.distance(it.position) < swing.hitFrontRadius + it.solidRadius &&
                    swing.hitBackPosition.distance(it.position) > swing.hitBackRadius
                ) {
                    it.damaged()
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