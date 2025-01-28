package net.mattemade.bossrush.scene

import com.littlekt.Context
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.shader.ShaderProgram
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.PI_F
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.radians
import com.littlekt.util.fastForEach
import com.littlekt.util.fastIterateRemove
import com.littlekt.util.seconds
import net.mattemade.bossrush.ARENA_RADIUS
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.DEBUG
import net.mattemade.bossrush.NO_ROTATION
import net.mattemade.bossrush.SOUND_VOLUME
import net.mattemade.bossrush.VIRTUAL_HEIGHT
import net.mattemade.bossrush.VIRTUAL_WIDTH
import net.mattemade.bossrush.input.GameInput
import net.mattemade.bossrush.math.minimalRotation
import net.mattemade.bossrush.math.rotateTowards
import net.mattemade.bossrush.objects.Boss
import net.mattemade.bossrush.objects.BossMeleeAttack
import net.mattemade.bossrush.objects.Cogwheel
import net.mattemade.bossrush.objects.Collectible
import net.mattemade.bossrush.objects.Column
import net.mattemade.bossrush.objects.Projectile
import net.mattemade.bossrush.objects.ReadyBall
import net.mattemade.bossrush.objects.SpikeBall
import net.mattemade.bossrush.objects.Swing
import net.mattemade.bossrush.objects.TemporaryDepthRenderableObject
import net.mattemade.bossrush.objects.Trap
import net.mattemade.bossrush.objects.boss.BossI
import net.mattemade.bossrush.objects.boss.BossII
import net.mattemade.bossrush.player.Player
import net.mattemade.bossrush.shader.ParticleFragmentShader
import net.mattemade.bossrush.shader.ParticleVertexShader
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self
import net.mattemade.utils.render.PixelRender
import kotlin.math.abs
import kotlin.time.Duration

class Fight(
    private val context: Context,
    private val input: GameInput,
    private val assets: Assets,
    private val particleShader: ShaderProgram<ParticleVertexShader, ParticleFragmentShader>,
    private val end: () -> Unit,
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

    private val player by lazy { Player(context, input, assets, particleShader, ::placingTrap, ::swing).solid() }
    private val arena by lazy {
        Arena(0f, assets, context, particleShader, { minOf(1f, totalBossHealth / maxBossHealth) }).also {
            delay(extraDelay) {
                Cogwheel(MutableVec2f(0f, 0f), player, input, assets, context, particleShader).solid()
                cogwheelScheduled = false
            }
        }
    }
    private var shapeRenderer: ShapeRenderer? = null

    private var bosses =
        mutableListOf<Boss>() // Boss(player, assets, { it.save() }, ::spawnCollectible, ::bossMeleeAttack).solid()

    private var gameRenderer = PixelRender(
        context,
        targetWidth = VIRTUAL_WIDTH,
        targetHeight = VIRTUAL_HEIGHT,
        preRenderCall = { dt, camera ->
            update(camera, dt)
        },
        renderCall = { dt, camera, batch ->
            render(batch)
        },
        worldWidth = VIRTUAL_WIDTH,
        worldHeight = VIRTUAL_HEIGHT,
        clear = true,
    )
    val texture = gameRenderer.texture
    val hud by lazy { Hud(context, particleShader, assets, player) }

    private var extraDelay = if (DEBUG) 0f else 5f // each retry it goes down to speed up animations
        set(value) {
            field = if (DEBUG) 0f else value
        }
    private var bossScheduled = false
    private var cogwheelScheduled = false
    private var maxBossHealth = 1f
    private var totalBossHealth = 1f
    val uiTexture by lazy { hud.uiTexture }

    private val tempVec2f = MutableVec2f()
    private val cameraStart = MutableVec2f()
    private val cameraTarget = MutableVec2f()
    private var cameraTime: Float = 2f
    private var cameraMaxTime: Float = 2f
    private var cameraFactor: Float = 1f

    private val delayedQueue = mutableListOf<Pair<Float, () -> Unit>>()

    private fun getDisplacementAt(position: Vec2f, dt: Duration): Vec2f {
        // TODO: apply active turn-tables too
        if (arena.disappearing) {
            tempVec2f.set(0f, 0f)
            return tempVec2f
        }
        tempVec2f.set(position).rotate((arena.angularVelocity * dt.seconds).radians).minusAssign(position)
        return tempVec2f
    }

    private fun delay(seconds: Float, action: () -> Unit) {
        delayedQueue += (absoluteTime + seconds) to action
    }

    private fun update(camera: Camera, dt: Duration) {
        delayedQueue.fastIterateRemove {
            val remove = absoluteTime >= it.first
            if (remove) {
                it.second()
            }
            remove
        }
        if (cameraTime < cameraMaxTime) {
            cameraTime = minOf(cameraMaxTime, cameraTime + dt.seconds)
            cameraFactor = cameraTime / cameraMaxTime
        }

        cameraTarget.set(player.position)
        bosses.fastForEach {
            if (it.importantForCamera) {
                cameraTarget.add(it.position)
            }
        }
        cameraTarget.scale(1f / (1f + bosses.size))

        camera.position.set(
            cameraStart.x + (cameraTarget.x - cameraStart.x) * cameraFactor,
            cameraStart.y + (cameraTarget.y - cameraStart.y) * cameraFactor,
            0f
        )
        context.audio.setListenerPosition(camera.position.x, camera.position.y, -50f)
        assets.sound.arenaRotating.setPosition(camera.position.x, camera.position.y)
        camera.update()

        gameObjects.fastIterateRemove { obj ->
            !obj.update(dt).also {
                if (!it) {
                    solidObjects.remove(obj)
                    if (obj is Boss) {
                        bosses.remove(obj)
                        camera.startMovement()
                    }
                }
            }
        }
        arena.adjustVelocity(player.previousPosition, player.position, 0.05f)
        arena.update(dt)
        bosses.fastForEach {
            it.applyRotation(arena.angularVelocity * dt.seconds * 2f)
        }

        solidObjects.fastForEach {
            if (it != player) {
                it.solidRadius?.let { solidRadius ->
                    if (player.position.distance(it.position) < player.solidRadius + solidRadius) {
                        if (it is Collectible && !it.collected) {
                            it.collected = true
                            player.resources++
                        } else if (player.damagedForSeconds == 0f && (it is SpikeBall || it is Boss && it.isDashing) && it.isActive()) {
                            player.bumpFrom(it.position)
                            player.damaged()
                        } else if (it.isActive()) { // non-dashing boss or column
                            if (player.damagedForSeconds > 0f && it is Boss && it.isDashing) {
                                // let the boss fly through
                            } else {
                                // push player away
                                tempVec2f.set(player.position).subtract(it.position)
                                    .setLength(player.solidRadius + solidRadius)
                                player.position.set(it.position).add(tempVec2f)
                            }
                        }
                    }
                    if (it !is Boss) {
                        bosses.forEach { boss ->
                            if (it.isActive() && it !is Collectible && boss.position.distance(it.position) < boss.solidRadius + solidRadius) {
                                // push boss away
                                tempVec2f.set(boss.position).subtract(it.position)
                                    .setLength(boss.solidRadius + solidRadius)
                                boss.position.set(it.position).add(tempVec2f)
                                if (boss.isDashing) {
                                    boss.trapped()
                                }

                            }
                        }
                    }
                }

            }
            // TODO: the same for boss? maybe let it go throw walls unless it does dash attack
        }
        var cogwheelFound = cogwheelScheduled
        gameObjects.fastIterateRemove { obj ->
            obj.displace(getDisplacementAt(position = obj.position, dt = dt))
            val result = when (obj) {
                is Trap -> {
                    if (obj.activatedTimeToLive < 0f) {
                        if (obj.position.distance(player.position) < player.solidRadius + obj.solidRadius) {
                            player.trapped()
                            obj.activate()
                        }
                        bosses.fastForEach { boss ->
                            if (obj.activatedTimeToLive < 0f && boss.solidElevation < 5f && obj.position.distance(boss.position) < boss.solidRadius + obj.solidRadius * 3f) {
                                boss.trapped()
                                obj.activate()
                            }
                        }
                    }
                    false // do not remove trap here
                }

                is Projectile -> {
                    if (obj.timeToLive >= 0f) {
                        // collision check
                        var collide = false
                        solidObjects.fastForEach { solid ->
                            val itIsBoss = bosses.contains(solid)
                            if (obj.canDamageBoss || !itIsBoss) { // do not collide with boss if we can't damage
                                solid.solidRadius?.let { solidRadius ->
                                    if (solid.position.distance(obj.position) < solidRadius + obj.solidRadius && obj.solidElevation >= solid.solidElevation
                                        && obj.solidElevation <= solid.solidElevation + solid.solidHeight
                                    ) {
                                        if (solid === player) {
                                            player.damaged()
                                            obj.onPlayerImpact(obj)
                                            collide = true
                                        } else if (solid is Boss && solid.isActive()) {
                                            solid.damage()
                                            collide = true
                                        } else if (solid.isActive() && (solid is Column || solid is SpikeBall)) {
                                            obj.direction.set(0f, 0f)
                                            assets.sound.projectileLand.play(
                                                volume = SOUND_VOLUME,
                                                positionX = obj.position.x,
                                                positionY = obj.position.y
                                            )
                                            obj.onSolidImpact(obj)
                                            //spawnCollectible(obj)
                                            collide = true
                                        } else if (solid is Collectible) {
                                            // noop
                                        }
                                    } else if (solid is Boss && solid.canSwing && solid.meleeCooldown == 0f && solid.position.distance(obj.position) < 20f) {
                                        val angle = tempVec2f.set(obj.position).subtract(solid.position)
                                            .angleTo(NO_ROTATION).radians
                                        swing(
                                            solid.position,
                                            angle,
                                            solid.solidElevation,
                                            clockwise = true,
                                            powerful = true,
                                            powerfulAngularSpeedScale = 2f
                                        ) {
                                            player::position to { 16f }
                                        }
                                        solid.meleeCooldown = 2f
                                    }
                                }
                            }

                        }
                        collide
                    } else {
                        false
                    }
                }

                is Cogwheel -> {
                    cogwheelFound = true
                    val rotationFactor = kotlin.math.abs(obj.rotation) / PI2_F
                    if (obj.isActive()) {
                        arena.setClockFactor(rotationFactor)
                    }
                    if (obj.isActive() && rotationFactor > 1f) {
                        arena.fadeClockOut()
                        destroyCollectibles(obj.position)
                        when (arena.currentHour) {
                            0 -> {
                                delay(extraDelay / 2f) {
                                    bosses.add(
                                        BossI(
                                            context,
                                            particleShader,
                                            player,
                                            assets,
                                            { it.save() },
                                            ::spawnCollectible,
                                            ::bossMeleeAttack,
                                            ::destroyCollectibles,
                                            { camera.startMovement() },
                                            /*MutableVec2f(0f, 100f),*/
                                            //health = 0.289f
                                        ).solid()
                                    )
                                    maxBossHealth = bosses.sumOf { it.health }
                                    totalBossHealth = maxBossHealth
                                    camera.startMovement()
                                    bossScheduled = false
                                }
                                bossScheduled = true
                            }

                            1 -> {
                                delay(extraDelay / 3f) {
                                    Column(
                                        MutableVec2f(-50f, 0f),
                                        assets,
                                        context,
                                        particleShader
                                    ).solid()
                                }
                                delay(extraDelay * 2f / 3f) {
                                    Column(
                                        MutableVec2f(50f, 0f),
                                        assets,
                                        context,
                                        particleShader
                                    ).solid()
                                }
                                delay(extraDelay) {
                                    bosses.add(
                                        BossII(
                                            context,
                                            particleShader,
                                            player,
                                            assets,
                                            { it.save() },
                                            ::spawnCollectible,
                                            ::bossMeleeAttack,
                                            ::destroyCollectibles,
                                            { camera.startMovement() },
                                            /*MutableVec2f(0f, -100f),
                                            health = 0.4f*/
                                        ).solid()
                                    )
                                    maxBossHealth = bosses.sumOf { it.health }
                                    totalBossHealth = maxBossHealth
                                    camera.startMovement()
                                    bossScheduled = false
                                }
                                bossScheduled = true
                            }

                            2 -> {
                                delay(extraDelay / 2f) {
                                    SpikeBall(
                                        context,
                                        particleShader,
                                        MutableVec2f(0f, 0f),
                                        60f,
                                        0.66f,
                                        assets,
                                        arena
                                    ).solid()
                                }
                                delay(extraDelay) {
                                    bosses.add(
                                        Boss(
                                            context,
                                            particleShader,
                                            player,
                                            assets,
                                            { it.save() },
                                            ::spawnCollectible,
                                            ::bossMeleeAttack,
                                            ::destroyCollectibles,
                                            MutableVec2f(-100f, 0f),
                                            health = 0.25f
                                        ).solid()
                                    )
                                    bosses.add(
                                        Boss(
                                            context,
                                            particleShader,
                                            player,
                                            assets,
                                            { it.save() },
                                            ::spawnCollectible,
                                            ::bossMeleeAttack,
                                            ::destroyCollectibles,
                                            MutableVec2f(100f, 0f),
                                            health = 0.25f
                                        ).solid()
                                    )
                                    maxBossHealth = bosses.sumOf { it.health }
                                    totalBossHealth = maxBossHealth
                                    camera.startMovement()
                                    bossScheduled = false
                                }
                                bossScheduled = true
                            }

                            3 -> {
                                for (i in 0..2) {
                                    val position = MutableVec2f(66f, 0f).rotate((PI2_F * i / 3f).radians)
                                    delay(extraDelay / 2f + i * 0.5f) {
                                        Column(
                                            position,
                                            assets,
                                            context,
                                            particleShader
                                        ).solid()
                                    }
                                    delay(extraDelay / 2f + i * 0.5f + 0.25f) {
                                        // TODO: taking advantage that Column will change the displacement of the same mutable position
                                        // might be too dangerous though, but meh ¦3
                                        SpikeBall(
                                            context,
                                            particleShader,
                                            position,
                                            33f,
                                            2f,
                                            assets,
                                            arena,
                                            0.5f
                                        ).solid()
                                    }
                                }
                                delay(extraDelay) {
                                    bosses.add(
                                        Boss(
                                            context,
                                            particleShader,
                                            player,
                                            assets,
                                            { it.save() },
                                            ::spawnCollectible,
                                            ::bossMeleeAttack,
                                            ::destroyCollectibles,
                                            MutableVec2f(0f, 100f),
                                            health = 0.3f
                                        ).solid()
                                    )
                                    maxBossHealth = bosses.sumOf { it.health }
                                    totalBossHealth = maxBossHealth
                                    camera.startMovement()
                                    bossScheduled = false
                                }
                                bossScheduled = true
                            }

                            4 -> {
                                // TODO: backwards bossrush, and then launch this
                                cogwheelScheduled = true // to prevent if from appearing
                                arena.currentHour-- // to reverse the advance, as there are no more
                                delay(extraDelay / 2f) {
                                    arena.turnToZero {
                                        delay(extraDelay / 2f) {
                                            arena.startDisappearing()
                                            delay(extraDelay * 1.25f) {
                                                player.startDisappearing()
                                                delay(extraDelay * 1.5f) {
                                                    end()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        camera.startMovement()
                        obj.startDisappearing()
                        false
                    } else {
                        false
                    }
                }

                /*is Boss -> {
                    val dead = obj.health <= 0f
                    if (dead) {
                        bosses.remove(obj)
                    }
                    dead
                }*/

                else -> false
            }
            if (result) {
                solidObjects.remove(obj)
            }
            result
        }
        if (bosses.isNotEmpty()) {
            totalBossHealth = maxOf(0f, bosses.sumOf { it.health })
        }
        val playerIsDead = player.hearts == 0
        if (!bossScheduled && !cogwheelFound && (totalBossHealth <= 0f || playerIsDead)) {
            bosses.clear()
            solidObjects.fastIterateRemove {
                var removing = it !is Player && it !is Collectible
                if (it is Column || it is SpikeBall || it is Boss) {
                    it.deactivate()
                    if (playerIsDead) {
                        it.startDisappearing()
                    } else {
                        delay(if (it is Boss) 1f else 2f, it::startDisappearing)
                    }
                    removing = false
                }
                if (removing) {
                    gameObjects.remove(it)
                }
                removing
            }
            gameObjects.fastIterateRemove {
                if (it is Projectile) {
                    spawnCollectible(it)
                    true
                } else {
                    false
                }
            }
            if (playerIsDead) {
                extraDelay = maxOf(0f, extraDelay - 2f)
                player.maxHearts = minOf(20, player.maxHearts + 1)
                player.hearts = player.maxHearts
                gameObjects.fastIterateRemove {
                    it is Projectile
                }
                delayedQueue.clear()
            } else {
                // bosses cleared
                extraDelay = 5f
                arena.currentHour++
            }
            maxBossHealth = 1f // reset the progress
            totalBossHealth = maxBossHealth
            camera.startMovement()
            delay(extraDelay) {
                var playerFound = false
                gameObjects.fastIterateRemove { // removing a duplicate player object, why is it here sometimes??
                    if (it is Player) {
                        if (playerFound) {
                            true
                        } else {
                            playerFound = true
                            false
                        }
                    } else if (it is Boss || it is Column || it is SpikeBall) { // also remove everything else except projectiles, just in case
                        true
                    } else {
                        false
                    }
                }
                solidObjects.fastIterateRemove { it is Boss || it is Column || it is SpikeBall }
                bosses.clear()
                Cogwheel(MutableVec2f(0f, 0f), player, input, assets, context, particleShader).solid()
                cogwheelScheduled = false
            }
            cogwheelScheduled = true
        } else if (playerIsDead) {
            // player is dead between stages, just restore 1 heart
            player.hearts = 1
        }
        arena.updateClock(dt)
    }

    private fun Camera.startMovement() {
        cameraStart.set(position.x, position.y)
        cameraFactor = 0f
        cameraTime = 0f
    }

    private fun render(batch: Batch) {
        (shapeRenderer ?: ShapeRenderer(batch, slice = assets.texture.whitePixel).also { shapeRenderer = it }).let { shapeRenderer ->
            arena.render(batch, shapeRenderer)
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
                player.hearts = minOf(player.maxHearts, player.hearts + (player.maxHearts + 1) / 2)
            } else if (seconds >= 1f && player.resources >= 5) {
                player.resources -= 5
                Trap(MutableVec2f(player.racketPosition), assets).save()
            } else if (seconds >= 0f && player.resources >= 2) {
                assets.sound.placeBall.play(
                    volume = SOUND_VOLUME,
                    positionX = player.racketPosition.x,
                    positionY = player.racketPosition.y
                )
                player.resources -= 2
                ReadyBall(MutableVec2f(player.racketPosition), assets).save()
            }
        }
    }

    private fun swing(angle: Float, clockwise: Boolean, powerful: Boolean) {
        swing(player.position, angle, 0f, clockwise, powerful, 1f) {
            if (powerful) {
                val ballRotation = it.direction.angleTo(NO_ROTATION).radians
                val targetBoss = bosses.minByOrNull { boss ->
                    tempVec2f.set(player.position).subtract(boss.position)
                    minimalRotation(
                        ballRotation,
                        tempVec2f.angleTo(NO_ROTATION).radians,
                    )
                }
                targetBoss?.let { boss ->
                    boss::position to { boss.solidElevation + boss.solidHeight / 2f }
                }
            } else {
                null
            }
        }
    }

    private fun swing(
        from: MutableVec2f,
        angle: Float,
        elevation: Float,
        clockwise: Boolean,
        powerful: Boolean,
        powerfulAngularSpeedScale: Float,
        getTarget: ((Projectile) -> Pair<() -> Vec2f, () -> Float>?)?
    ) {
        if (powerful) {
            assets.sound.strongSwing.sound.play(volume = 20f, positionX = from.x, positionY = from.y)
        } else {
            assets.sound.lightSwing.sound.play(volume = 20f, positionX = from.x, positionY = from.y)
        }
        val swing = Swing(from, angle, elevation, clockwise, assets, powerful).save()
        gameObjects.fastForEach {
            if (it is Projectile) {
                if (it.isReversible && abs(it.solidElevation - elevation) <= 30f && swing.hitFrontPosition.distance(it.position) < swing.hitFrontRadius &&
                    swing.hitBackPosition.distance(it.position) > swing.hitBackRadius
                ) {
                    if (it.isBomb) {
                        it.onSolidImpact(it)
                        it.timeToLive = 0.00000001f
                    } else {
                        it.canDamageBoss = true
                        it.timeToLive = 0f
                        if (powerful) {
                            tempVec2f.set(it.position).subtract(from)
                                .rotate((if (clockwise) -PI_F * 0.4f else PI_F * 0.4f).radians)
                            it.direction.setLength(120f).rotateTowards(tempVec2f)
                            val target = getTarget?.invoke(it)
                            it.target = target?.first
                            it.targetElevation = target?.second
                            it.angularSpeedScale = powerfulAngularSpeedScale
                        } else {
                            tempVec2f.set(it.position).subtract(from)
                            it.direction.setLength(80f).rotateTowards(tempVec2f)
                            it.elevationRate = -it.elevationRate
                        }
                    }
                }
            } else if (it is Boss && it.isActive()) {
                if (it.solidElevation < 20f && swing.hitFrontPosition.distance(it.position) < swing.hitFrontRadius + it.solidRadius &&
                    swing.hitBackPosition.distance(it.position) > swing.hitBackRadius
                ) {
                    it.damage(strong = powerful)
                }
            } else if (it is ReadyBall && swing.hitFrontPosition.distance(it.position) < swing.hitFrontRadius &&
                swing.hitBackPosition.distance(it.position) > swing.hitBackRadius
            ) {
                gameObjects.remove(it)
                tempVec2f.set(it.position).subtract(from)
                    .rotate((if (clockwise) -PI_F * 0.4f else PI_F * 0.4f).radians)
                val ballDirection = MutableVec2f(200f, 0f).also { it.rotateTowards(tempVec2f) }
                val ballRotation = ballDirection.angleTo(NO_ROTATION).radians
                val targetBoss = bosses.minByOrNull { boss ->
                    tempVec2f.set(from).subtract(boss.position)
                    minimalRotation(
                        ballRotation,
                        tempVec2f.angleTo(NO_ROTATION).radians,
                    )
                }
                Projectile(
                    assets = assets,
                    texture = assets.texture.projectile,
                    position = it.position.toMutableVec2(),
                    direction = ballDirection,
                    solidElevation = it.elevation,
                    elevationRate = 120f,
                    onSolidImpact = ::spawnCollectible,
                    scale = 0.5f,
                ).also {
                    targetBoss?.let { boss ->
                        it.target = boss::position
                        it.targetElevation = { boss.solidElevation + boss.solidHeight / 2f }
                    }
                    it.angularSpeedScale = 1f
                    it.canDamageBoss = true
                }.save()
            }
        }
    }

    private fun bossMeleeAttack(position: Vec2f, angle: Float, clockwise: Boolean) {
        BossMeleeAttack(position, angle, clockwise, assets).save()
    }

    private fun spawnCollectible(projectile: Projectile) {
        if (projectile.position.length() < ARENA_RADIUS) {
            Collectible(
                position = projectile.position.toMutableVec2(),
                direction = projectile.direction.toMutableVec2().scale(0.5f),
                elevation = maxOf(0f, projectile.solidElevation),
                //targetElevation = 6f,
            ).solid()
        }
    }

    private fun destroyCollectibles(by: Boss) {
        destroyCollectibles(by.position)
    }

    private fun destroyCollectibles(from: Vec2f) {
        gameObjects.fastForEach {
            if (it is Collectible) {
                delay(tempVec2f.set(from).subtract(it.position).length() / 400f) {
                    it.startDisappearing()
                }
            }
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

    private inline fun <T> Iterable<T>.sumOf(selector: (T) -> Float): Float {
        var sum: Float = 0f
        for (element in this) {
            sum += selector(element)
        }
        return sum
    }
}