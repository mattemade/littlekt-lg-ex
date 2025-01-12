package net.mattemade.bossrush.scene

import com.littlekt.Context
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.input.InputMapController
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.PI_F
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.radians
import com.littlekt.math.isFuzzyEqual
import com.littlekt.util.fastForEach
import com.littlekt.util.fastIterateRemove
import com.littlekt.util.seconds
import net.mattemade.bossrush.ARENA_RADIUS
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.GAME_WIDTH
import net.mattemade.bossrush.UI_WIDTH
import net.mattemade.bossrush.VIRTUAL_HEIGHT
import net.mattemade.bossrush.VIRTUAL_WIDTH
import net.mattemade.bossrush.input.GameInput
import net.mattemade.bossrush.math.rotateTowards
import net.mattemade.bossrush.objects.BossMeleeAttack
import net.mattemade.bossrush.objects.Collectible
import net.mattemade.bossrush.objects.Column
import net.mattemade.bossrush.objects.Projectile
import net.mattemade.bossrush.objects.Swing
import net.mattemade.bossrush.objects.TemporaryDepthRenderableObject
import net.mattemade.bossrush.objects.TestBoss
import net.mattemade.bossrush.objects.Trap
import net.mattemade.bossrush.player.Player
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.PixelRender
import kotlin.time.Duration

class Fight(
    private val context: Context,
    private val input: InputMapController<GameInput>,
    private val assets: Assets,
) : Releasing by Self() {

    private val gameObjects = mutableListOf<TemporaryDepthRenderableObject>()
    private val solidObjects = mutableListOf<TemporaryDepthRenderableObject>()
    private var absoluteTime: Float = 0f

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
        Column(MutableVec2f(50f, 0f), assets).solid()
        Arena(0f, assets)
    }
    private var shapeRenderer: ShapeRenderer? = null
    private var uiShapeRenderer: ShapeRenderer? = null


    private val testBoss by lazy {
        TestBoss(player, assets, { it.save() }, ::spawnCollectible, ::bossMeleeAttack).solid()
    }

    private var gameRenderer = PixelRender(
        context,
        targetWidth = GAME_WIDTH,
        targetHeight = VIRTUAL_HEIGHT,
        preRenderCall = { dt, camera ->
            update(camera, dt)
        },
        renderCall = { dt, camera, batch ->
            render(batch)
        },
        worldWidth = GAME_WIDTH,
        worldHeight = VIRTUAL_HEIGHT,
        clear = true,
    )
    val texture = gameRenderer.texture
    private var uiRenderer = PixelRender(
        context,
        targetWidth = UI_WIDTH,
        targetHeight = VIRTUAL_HEIGHT,
        preRenderCall = { dt, camera ->
            camera.position.set(UI_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0f)
            camera.update()
        },
        renderCall = { dt, camera, batch ->
            renderUi(batch)
        },
        worldWidth = UI_WIDTH,
        worldHeight = VIRTUAL_HEIGHT,
        clear = true,
    )
    val uiTexture = uiRenderer.texture

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
                        if (it is Collectible && !it.collected) {
                            it.collected = true
                            player.resources++
                        } else {
                            // push player away
                            tempVec2f.set(player.position).subtract(it.position)
                                .setLength(player.solidRadius + solidRadius)
                            player.position.set(it.position).add(tempVec2f)
                        }
                    }
                }
            }
            // TODO: the same for boss? maybe let it go throw walls unless it does dash attack
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
                        if (testBoss.elevation < 5f && it.position.distance(testBoss.position) < testBoss.solidRadius + it.solidRadius * 3f) {
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
                                        if (solid === player) {
                                            player.damaged()
                                            collide = true
                                        } else if (solid === testBoss) {
                                            if (it.elevation.isFuzzyEqual(
                                                    testBoss.elevation,
                                                    eps = testBoss.solidRadius
                                                )
                                            ) {
                                                testBoss.damaged()
                                                collide = true
                                            }
                                        } else if (solid is Column) {
                                            it.direction.set(0f, 0f)
                                            spawnCollectible(it)
                                            collide = true
                                        } else if (solid is Collectible) {
                                            // noop
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

    private fun renderUi(batch: Batch) {
        (uiShapeRenderer ?: ShapeRenderer(batch).also { uiShapeRenderer = it }).let { shapeRenderer ->

            for (i in 0..3) {
                batch.draw(if (i == 3) assets.texture.heartEmpty else assets.texture.heartFilled, x = 10f + 9f * i, y = 7f, width = 8f, height = 6f)
            }

            batch.draw(assets.texture.clockBg, x = 24f, y = 24f, width = 57f, height = 57f)
            val totalMinutes = absoluteTime// * 20f
            val minutes = totalMinutes % 60f
            val hours = totalMinutes / 60f
            val minutesRotation = minutes * PI2_F / 60f// + PI_F/2f
            val hoursRotation = hours * PI2_F / 4f// + PI_F/2f
            tempVec2f.set(-57f / 2f, -57 / 2f).rotate(hoursRotation.radians)
            batch.draw(assets.texture.clockHour, x = 24f + 57 / 2f + tempVec2f.x, y = 24f + 57f / 2f + tempVec2f.y, width = 57f, height = 57f, rotation = hoursRotation.radians)
            tempVec2f.set(-57f / 2f, -57 / 2f).rotate(minutesRotation.radians)
            batch.draw(assets.texture.clockMinute, x = 24f + 57 / 2f + tempVec2f.x, y = 24f + 57f / 2f + tempVec2f.y, width = 57f, height = 57f, rotation = minutesRotation.radians)
            batch.draw(assets.texture.mock, x = 0f, y = 90f, width = 100f, height = 150f)
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
                        tempVec2f.set(it.position).subtract(player.position)
                            .rotate((if (clockwise) -PI_F * 0.4f else PI_F * 0.4f).radians)
                        it.direction.scale(1.25f).rotateTowards(tempVec2f)
                        it.target = testBoss::position
                        it.targetElevation = testBoss::elevation
                        it.angularSpeedScale = 2f
                    } else {
                        tempVec2f.set(it.position).subtract(player.position)
                        it.direction.rotateTowards(tempVec2f)
                    }
                }
            } else if (it is TestBoss) {
                if (it.elevation < 20f && swing.hitFrontPosition.distance(it.position) < swing.hitFrontRadius + it.solidRadius &&
                    swing.hitBackPosition.distance(it.position) > swing.hitBackRadius
                ) {
                    it.damaged()
                }
            }
        }
    }

    private fun bossMeleeAttack(angle: Float, clockwise: Boolean) {
        BossMeleeAttack(testBoss.position, angle, clockwise, assets).save()
    }

    private fun spawnCollectible(projectile: Projectile) {
        if (projectile.position.length() < ARENA_RADIUS) {
            Collectible(
                position = projectile.position.toMutableVec2(),
                direction = projectile.direction.toMutableVec2().scale(0.5f),
                elevation = projectile.elevation,
                targetElevation = 6f,
            ).solid()
        }
    }

    fun updateAndRender(dt: Duration) {
        absoluteTime += dt.seconds
        gameRenderer.render(dt)
        uiRenderer.render(dt)
    }

    fun resize(width: Int, height: Int) {
        //renderer.resize(width, height, 320f, 240f)
    }
}