package net.mattemade.bossrush.scene

import com.littlekt.Context
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.shader.ShaderProgram
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
import net.mattemade.bossrush.input.GameInput
import net.mattemade.bossrush.math.rotateTowards
import net.mattemade.bossrush.objects.BossMeleeAttack
import net.mattemade.bossrush.objects.Collectible
import net.mattemade.bossrush.objects.Column
import net.mattemade.bossrush.objects.Projectile
import net.mattemade.bossrush.objects.ReadyBall
import net.mattemade.bossrush.objects.Swing
import net.mattemade.bossrush.objects.TemporaryDepthRenderableObject
import net.mattemade.bossrush.objects.TestBoss
import net.mattemade.bossrush.objects.TextureParticles
import net.mattemade.bossrush.objects.Trap
import net.mattemade.bossrush.player.Player
import net.mattemade.bossrush.shader.ParticleFragmentShader
import net.mattemade.bossrush.shader.ParticleVertexShader
import net.mattemade.bossrush.shader.Particler
import net.mattemade.utils.math.fill
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.PixelRender
import kotlin.time.Duration

class Fight(
    private val context: Context,
    private val input: InputMapController<GameInput>,
    private val assets: Assets,
    private val particleShader: ShaderProgram<ParticleVertexShader, ParticleFragmentShader>,
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
    val hud by lazy { Hud(context, particleShader, assets, player, testBoss) }
    val uiTexture by lazy { hud.uiTexture }

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

    private fun placingTrap(seconds: Float, place: Boolean) {
        if (place) {
            if (seconds >= 2f && player.resources >= 10) {
                // heal
                player.resources -= 10
                player.hearts = maxOf(5, player.hearts + 1)
                // TODO: play healing animation
            } else if (seconds >= 1f && player.resources >= 5) {
                player.resources -= 10
                Trap(MutableVec2f(player.racketPosition), assets).save()
            } else if (seconds >= 0f && player.resources >= 2) {
                ReadyBall(MutableVec2f(player.racketPosition)).save()
            }
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
                        it.angularSpeedScale = 1f
                    } else {
                        tempVec2f.set(it.position).subtract(player.position)
                        it.direction.rotateTowards(tempVec2f)
                    }
                }
            } else if (it is TestBoss) {
                if (it.elevation < 20f && swing.hitFrontPosition.distance(it.position) < swing.hitFrontRadius + it.solidRadius &&
                    swing.hitBackPosition.distance(it.position) > swing.hitBackRadius
                ) {
                    it.damaged(strong = powerful)
                }
            } else if (it is ReadyBall && swing.hitFrontPosition.distance(it.position) < swing.hitFrontRadius &&
                swing.hitBackPosition.distance(it.position) > swing.hitBackRadius) {
                gameObjects.remove(it)
                tempVec2f.set(it.position).subtract(player.position)
                    .rotate((if (clockwise) -PI_F * 0.4f else PI_F * 0.4f).radians)
                Projectile(
                    position = it.position.toMutableVec2(),
                    direction = MutableVec2f(200f, 0f).also { it.rotateTowards(tempVec2f) },
                    elevation = it.elevation,
                    elevationRate = 120f,
                    spawnCollectible = ::spawnCollectible,
                ).also{
                    it.target = { testBoss.position }
                    it.targetElevation = { testBoss.elevation }
                    it.angularSpeedScale = 1f
                    it.canDamageBoss = true
                }.save()
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
                //targetElevation = 6f,
            ).solid()
        }
    }

    fun updateAndRender(dt: Duration) {
        absoluteTime += dt.seconds
        gameRenderer.render(dt)
        hud.updateAndRender(dt)
    }

    fun resize(width: Int, height: Int) {
        //renderer.resize(width, height, 320f, 240f)
    }
}