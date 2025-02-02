package net.mattemade.bossrush.objects

import com.littlekt.Context
import com.littlekt.audio.AudioClipEx
import com.littlekt.audio.AudioStreamEx
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.PI_F
import com.littlekt.math.Vec2f
import com.littlekt.math.floorToInt
import com.littlekt.math.geom.radians
import com.littlekt.math.geom.times
import com.littlekt.util.fastForEach
import com.littlekt.util.milliseconds
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.DEBUG
import net.mattemade.bossrush.NO_ROTATION
import net.mattemade.bossrush.math.minimalRotation
import net.mattemade.bossrush.maybePlay
import net.mattemade.bossrush.player.Player
import net.mattemade.bossrush.shader.ParticleShader
import net.mattemade.utils.math.fill
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration

typealias Program = List<Pair<Float, () -> Unit>>
typealias State = List<Pair<Float, Program>>

open class Boss(
    private val context: Context,
    private val shader: ParticleShader,
    private val player: Player,
    private val assets: Assets,
    private val spawn: (Projectile) -> Unit,
    private val spawnCollectible: (Projectile) -> Unit,
    private val melee: (position: Vec2f, angle: Float, clockwise: Boolean) -> Unit,
    private val destroyCollectibles: (Boss) -> Unit,
    val associatedMusic: AudioStreamEx,
    override val position: MutableVec2f = MutableVec2f(0f, -100f),
    var health: Float = 1f,
    private val standTexture: TextureSlice = assets.texture.bossIStand,
    private val flyTexture: TextureSlice = assets.texture.bossIFly,
    private val projectileTexture: TextureSlice = assets.texture.projectile,
) : TemporaryDepthRenderableObject {


    protected var nextFireIn = 0f
    protected var fireEvery = 0f
        set(value) {
            field = value
            nextFireIn = value
        }
    protected var periodicShot: () -> Unit = ::simpleAttack
    private val notSpawnCollectible: (Projectile) -> Unit = {}
    private var damagedForSeconds: Float = 0f
    private var starTimer = 0f
    protected var trappedForSeconds = 0f
    var meleeCooldown = 0f
    private val shadowRadii = MutableVec2f(0f, 0f)
    private val damageColor = Color.RED.toFloatBits()
    override var solidElevation: Float = 0f
    override val solidHeight: Float = 38f
    private var dizziness = 0f
    private var dizzinessSign = 1f
    var canMeleeAttack: Boolean = false
    var meleeAttackRadius = 40f
    var difficulty = 1f
    var canSwing: Boolean = false

    override val solidRadius: Float
        get() = 8f
    protected val tempVec2f = MutableVec2f()
    protected var dashingTowards: Vec2f? = null
    protected var dashingSpeed: Float = 80f
    var isDashing: Boolean = false
    protected open val verticalOffset: Float = 0f

    val jump = 0.25f to {
        assets.sound.bossJump.maybePlay(position)
        elevatingRate = 200f
        targetElevation = 200f
    }
    val smallJump = 0.25f to {
        elevatingRate = 100f
        targetElevation = 200f
    }
    val fall = listOf(0.25f to {
        elevatingRate = -200f
        targetElevation = 0f
    }, 0f to {
        assets.sound.bossLand.maybePlay(position)
        Unit
    }).toTypedArray()

    val smallFall = 0.25f to {
        elevatingRate = -100f
        targetElevation = 0f
    }
    protected val bombParticles by lazy { assets.texture.bombParticles.slice(15, 15, 0)[0] }

    init {
        assets.sound.assemble.maybePlay(position)
    }

    private val appearingFor = if (DEBUG) 0f else 3500f
    private fun createParticles(texture: TextureSlice): TextureParticles {
        val width = texture.width
        val widthFloat = texture.width.toFloat()
        val height = texture.height
        val heightFloat = height.toFloat()
        val halfWidth = width / 2f
        val halfHeight = height / 2f

        return TextureParticles(
            context,
            shader,
            texture,
            position,
            interpolation = 2,
            activeFrom = { x, y -> Random.nextFloat() * 300f + (height - y) * 30f },
            activeFor = { x, y -> 2000f },
            timeToLive = appearingFor,
            setStartColor = { a = 0f },
            setEndColor = { a = 1f },
            setStartPosition = { x, y ->
                fill(-width * 2f + width * 4f * Random.nextFloat(), y - heightFloat * 4f)
            },
            setEndPosition = { x, y ->
                fill(x - halfWidth, y - heightFloat * 0.8f - solidElevation) // normal rendering offsets
            },
        )
    }

    private var appearing = true
    private var disappearing = false
    private var deactivated = false
        set(value) {
            if (value && !field && solidElevation > 0f) {
                appear = createParticles(flyTexture)
                appear.addToTime(appearingFor)
            }
            field = value
        }

    private var appear = createParticles(standTexture)

    protected open val returnToPosition: State =
        listOf(
            1f to listOf(
                2f to {
                    dashingTowards = tempVec2f.set(position).setLength(100f).toVec2()
                    dashingSpeed = 80f
                },
                0f to {
                    currentState = stayingUpState
                },
            )
        )

    // program is a list of actions to choose from: "relative chance" to ("cooldown" to "action")
    private val stayingUpState: State =
        listOf(
            1f to listOf(
                1f to ::throwBomb,
                1f to ::throwBomb,
                1f to ::throwBomb,
            ),
            1f to listOf(
                //2f to ::spinClockwise,
                //0f to ::stopSpinning,
                2f to ::startCharging,
                0f to ::stopCharging,
                10f to ::dashIntoPlayer,
                2f to {}, // wait
            )
            /*1f to listOf(
                2f to ::stopCharging, // wait
                2f to ::spinClockwise,
                0f to ::stopSpinning,
                2f to ::startCharging,
                0f to ::stopCharging,
                10f to {
                    val angleToCenter = position.angleTo(NO_ROTATION).radians
                    val angleHorde = tempVec2f.set(position).subtract(player.position).angleTo(NO_ROTATION).radians
                    val diff = minimalRotation(angleToCenter, angleHorde)
                    val rotationAngle = PI_F - diff * 2f
                    tempVec2f.set(position).rotate((-rotationAngle).radians)
                    dashingTowards = tempVec2f.toVec2()
                    dashingSpeed = 200f
                }
            ),*/
            /*1f to listOf(
                1f to ::followPlayer,
                0f to ::stopFollowing,
            ),*/
            /* 0.25f to listOf(
                 2f to ::shotgun,
             ),
             0.25f to listOf(
                 1f to ::throwBoulder,
             ),
             0.1f to listOf(
                 0.25f to {
                     elevatingRate = 200f
                     targetElevation = 200f
                 },
                 0.25f to {
                     elevatingRate = -200f
                     targetElevation = 0f
                 },
                 4f to {
                     destroyCollectibles(this)
                     spawnBoulders()
                 }
             ),
             0.5f to listOf(
                 2f to ::spinClockwise,
                 0f to ::stopSpinning,
                 1f to ::elevate,
                 1f to ::simpleAttack,
                 1f to ::simpleAttack,
                 1f to ::land
             ),
             0.5f to listOf(
                 1f to ::spinCounterClockwise,
                 0f to ::stopSpinning,
                 1f to ::elevate,
                 0.5f to ::simpleAttack,
                 0.5f to ::simpleAttack,
                 2f to ::strongAttack,
                 1f to ::land
             )*/
        )

    private fun State.getWeightedRandomProgram(): Program {
        var sum = 0f
        fastForEach { sum += it.first }
        sum *= Random.nextFloat()
        for (i in indices) {
            val currentItem = this[i]
            sum -= currentItem.first
            if (sum <= 0f) {
                return currentItem.second
            }
        }
        return last().second
    }

    private val charge = TextureParticles(
        context, shader, assets.texture.whitePixel, position.toMutableVec2(),
        activeFrom = { x, y -> Random.nextFloat() * 1000f },
        activeFor = { x, y -> 1000f },
        timeToLive = 2000f,
        doubles = 2000,
        interpolation = 0,
        setStartColor = { a = 0f },
        setEndColor = { a = 1f },
        setStartPosition = { x, y ->
            val length = 200f + Random.nextFloat() * 500f
            val angle = Random.nextFloat() * PI2_F
            fill(length * cos(angle), length * sin(angle))
        },
        setEndPosition = { x, y -> fill(0f, standTexture.width / 2f) })
    protected var charging = false
    protected var chargingTimeMultiplier = 1f

    protected open var currentState: State = stayingUpState
        set(value) {
            field = value
            currentProgram = value.getWeightedRandomProgram()
            currentProgramIndex = -1
            toNextProgramIndex = 0f
        }
    private var currentProgram: Program = currentState.getWeightedRandomProgram()
    private var currentProgramIndex = -1
    private var toNextProgramIndex = 0f

    var angularSpinningSpeed = 0f
    protected var targetElevation = 0f
    var elevatingRate = 0f
    protected var followingPlayer = false
    protected var followingPlayerSpeed = 20f
    var importantForCamera = true

    protected fun floatUp() {
        targetElevation = 20f
        elevatingRate = 20f
    }

    protected fun floatDown() {
        targetElevation = 0f
        elevatingRate = -20f
    }

    protected fun dashIntoPlayer() {
        assets.sound.strongSwing.maybePlay(position)
        val angleToCenter = position.angleTo(NO_ROTATION).radians
        val angleHorde = tempVec2f.set(position).subtract(player.position).angleTo(NO_ROTATION).radians
        val diff = minimalRotation(angleToCenter, angleHorde)
        val rotationAngle = PI_F - diff * 2f
        tempVec2f.set(position).rotate((-rotationAngle).radians)
        dashingTowards = tempVec2f.toVec2()
        isDashing = true
        dashingSpeed = 200f
    }

    protected fun startChargingOverride(sound: AudioClipEx) {
        sound.maybePlay(position)
        charge.addToTime(-200000f)
        charging = true
    }

    protected fun startCharging() {
        startChargingOverride(assets.sound.charging)
    }

    protected fun stopCharging() {
        charging = false
    }

    protected fun followPlayer() {
        followingPlayer = true
    }

    protected fun stopFollowing() {
        followingPlayer = false
    }

    protected fun spinClockwise() {
        angularSpinningSpeed = 1f
    }

    protected fun spinCounterClockwise() {
        angularSpinningSpeed = -1f
    }

    protected fun stopSpinning() {
        angularSpinningSpeed = 0f
    }

    fun spawnBoulders() {
        for (i in 0..8) {
            tempVec2f.set(Random.nextFloat() * 100f, 0f).rotate((Random.nextFloat() * PI2_F).radians)
            val spawnElevation = 400f + Random.nextFloat() * 200f
            spawn(
                Projectile(
                    assets = assets,
                    texture = assets.texture.boulder,
                    position = tempVec2f.toMutableVec2(),
                    direction = MutableVec2f(0f, 0f),
                    solidElevation = spawnElevation,
                    elevationRate = 0f,
                    onSolidImpact = {
                        fireProjectiles(
                            4,
                            PI2_F,
                            it.position,
                            60f,
                            it.solidElevation + 6f,
                            50f,
                            100f,
                            false,
                            scale = 1f,
                            texture = assets.texture.stone,
                            onSpawnSound = assets.sound.silence,
                        )
                    },
                    gravity = 100f,
                    solidRadius = 8f,
                    isReversible = false,
                    onLandSound = assets.sound.boulderLand,
                )
            )
        }
    }

    fun spawnBombs() {
        for (i in 0..8) {
            spawnRandomFloatingBomb(200f, 200f)
        }
    }

    protected fun spawnRandomFloatingBomb(from: Float, range: Float) {
        tempVec2f.set(Random.nextFloat() * 100f, 0f).rotate((Random.nextFloat() * PI2_F).radians)
        val spawnElevation = from + Random.nextFloat() * range
        spawn(
            Projectile(
                assets = assets,
                texture = assets.texture.bomb,
                position = tempVec2f.toMutableVec2(),
                direction = MutableVec2f(0f, 0f),
                solidElevation = spawnElevation,
                elevationRate = 0f,
                onSolidImpact = {
                    fireProjectiles(
                        8,
                        PI2_F,
                        it.position,
                        80f,
                        maxOf(0f, it.solidElevation) + 0.1f,
                        elevationRateOverride = 10f,
                        tracking = false,
                        scale = 1f,
                        isReversible = false,
                        spawnsCollectible = false,
                        timeToLive = 0.3f,
                        texture = bombParticles[0],
                        animation = bombParticles,
                        timePerFrame = 0.3f / 5f,
                        onSpawnSound = assets.sound.silence,
                        onLandSound = assets.sound.silence,
                    )
                    /*fireProjectiles(
                        32,
                        PI2_F,
                        it.position,
                        60f,
                        maxOf(0f, it.solidElevation) + 0.1f,
                        elevationRateOverride = 10f,
                        tracking = false,
                        scale = 0.5f,
                        isReversible = false,
                        spawnsCollectible = false,
                        timeToLive = 0.3f,
                        texture = assets.texture.projectile
                    )*/
                },
                gravity = 50f,
                solidRadius = 2f,
                isBomb = true,
                scale = 1f,
                onLandSound = assets.sound.bombExplosion,
            )
        )
    }

    protected fun throwBoulder() {
        assets.sound.boulderThrow.maybePlay(position)
        val speed = 60f
        val distance = tempVec2f.set(player.position).subtract(position).length()
        val reachingInSeconds = distance / speed
        tempVec2f.setLength(speed)
        val spawnElevation = solidElevation + 24f
        spawn(
            Projectile(
                assets = assets,
                texture = assets.texture.boulder,
                position = position.toMutableVec2(),
                direction = MutableVec2f(tempVec2f),
                solidElevation = spawnElevation,
                elevationRate = 100f * reachingInSeconds,//-spawnElevation / reachingInSeconds * 0.7f, // to make them fly a bit longer,
                onSolidImpact = {
                    fireProjectiles(
                        4,
                        PI2_F,
                        it.position,
                        60f,
                        it.solidElevation + 6f,
                        50f,
                        100f,
                        false,
                        scale = 1f,
                        texture = assets.texture.stone,
                        onSpawnSound = assets.sound.silence,
                    )
                },
                gravity = 200f,
                solidRadius = 8f,
                isReversible = false,
                onLandSound = assets.sound.boulderLand,
            )
        )
    }


    protected fun throwBomb() {
        assets.sound.boulderThrow.maybePlay(position)
        val speed = 60f
        val distance = tempVec2f.set(player.position).subtract(position).length()
        val reachingInSeconds = distance / speed
        tempVec2f.setLength(speed)
        val spawnElevation = solidElevation + 24f
        makeBomb(spawnElevation, 50f * reachingInSeconds, MutableVec2f(tempVec2f))
    }

    protected fun makeBomb(spawnElevation: Float, elevationRate: Float, direction: MutableVec2f) {
        spawn(
            Projectile(
                assets = assets,
                texture = assets.texture.bomb,
                position = position.toMutableVec2(),
                direction = direction,
                solidElevation = spawnElevation,
                elevationRate = elevationRate,
                onSolidImpact = {
                    fireProjectiles(
                        8,
                        PI2_F,
                        it.position,
                        80f,
                        maxOf(0f, it.solidElevation) + 0.1f,
                        elevationRateOverride = 10f,
                        tracking = false,
                        scale = 1f,
                        isReversible = false,
                        spawnsCollectible = false,
                        timeToLive = 0.3f,
                        texture = bombParticles[0],
                        animation = bombParticles,
                        timePerFrame = 0.3f / 5f,
                        onSpawnSound = assets.sound.silence,
                        onLandSound = assets.sound.silence,
                    )
                    /*fireProjectiles(
                        32,
                        PI2_F,
                        it.position,
                        60f,
                        maxOf(0f, it.solidElevation) + 0.1f,
                        elevationRateOverride = 10f,
                        tracking = false,
                        scale = 0.5f,
                        isReversible = false,
                        spawnsCollectible = false,
                        timeToLive = 0.3f,
                        texture = assets.texture.projectile
                    )*/
                },
                gravity = 100f,
                solidRadius = 2f,
                isBomb = true,
                scale = 1f,
                onLandSound = assets.sound.bombExplosion,
            )
        )
    }

    protected fun shotgun() = fireProjectiles(
        count = 8,
        angle = PI_F / 4f,
        tracking = true,
        speed = 120f,
        elevation = solidHeight * 0.75f,
        elevationRateOverride = -solidElevation * 2f,
        scale = 1f,
        timeToLive = 0.6f,
        texture = assets.texture.bullet,
        rotating = true,
        onSpawnSound = assets.sound.shotgun,
    )

    protected fun simpleAttack() = fireProjectiles(1, 0f, tracking = true /*scale = 0.5f*/)

    protected fun strongAttack() = fireProjectiles(5, PI_F / 2f, tracking = true /*scale = 0.5f*/)

    protected fun fireProjectiles(
        count: Int,
        angle: Float,
        from: Vec2f = position,
        speed: Float = 80f,
        elevation: Float = solidElevation + 24f,
        elevationRateOverride: Float? = null,
        gravity: Float? = null,
        tracking: Boolean = true,
        scale: Float = 1f,
        timeToLive: Float = 0f,
        isReversible: Boolean = true,
        spawnsCollectible: Boolean = true,
        texture: TextureSlice = projectileTexture,
        extraAngle: Float = 0f,
        rotating: Boolean = false,
        animation: Array<TextureSlice>? = null,
        timePerFrame: Float = 0.2f,
        onSpawnSound: AudioClipEx = assets.sound.bossFire,
        onLandSound: AudioClipEx = assets.sound.projectileLand,
    ) {
        onSpawnSound.maybePlay(position)

        val deltaAngle = (angle / count).radians
        val startAngle = (-count / 2).toFloat() * deltaAngle

        val distance = tempVec2f.set(player.position).subtract(from).length()
        val reachingInSeconds = distance / speed
        tempVec2f.setLength(speed)
            .rotate((if (tracking) startAngle else -tempVec2f.angleTo(NO_ROTATION)) + extraAngle.radians)
        /*if (tracking) {
            tempVec2f.rotate(startAngle)
        }*/
        for (i in 0 until count) {
            spawn(
                Projectile(
                    assets = assets,
                    texture = texture,
                    position = from.toMutableVec2(),
                    direction = MutableVec2f(tempVec2f),
                    solidElevation = elevation,
                    elevationRate = elevationRateOverride
                        ?: (-elevation / reachingInSeconds * 0.7f), // to make them fly a bit longer,
                    onSolidImpact = if (spawnsCollectible) spawnCollectible else notSpawnCollectible,
                    gravity = gravity ?: 0f,
                    scale = scale,
                    timeToLive = timeToLive,
                    isReversible = isReversible,
                    rotating = rotating,
                    animationSlices = animation,
                    timePerFrame = timePerFrame,
                    onLandSound = onLandSound,
                )
            )
            tempVec2f.rotate(deltaAngle)
        }
    }

    open fun preUpdate() {

    }

    override fun update(dt: Duration): Boolean {
        preUpdate()
        starTimer = (starTimer + dt.seconds) % PI2_F
        if (disappearing) {
            appear.update(-dt)
            disappearing = appear.liveFactor > 0f
            shadowRadii.set(solidRadius + 2f, 5f).scale(appear.liveFactor)
            return disappearing
        } else if (appearing) {
            appearing = appear.update(dt)
            shadowRadii.set(solidRadius + 2f, 5f)
            if (appearing) {
                shadowRadii.scale(appear.liveFactor)
            }
            return true
        }

        if (damagedForSeconds > 0f) {
            damagedForSeconds = maxOf(0f, damagedForSeconds - dt.seconds)
        }
        if (deactivated) {
            return true
        }
        if (meleeCooldown > 0f) {
            meleeCooldown = maxOf(0f, meleeCooldown - dt.seconds * difficulty)
        }

        if (solidElevation < targetElevation && elevatingRate > 0f) {
            solidElevation = minOf(targetElevation, solidElevation + elevatingRate * dt.seconds)
        } else if (solidElevation > targetElevation && elevatingRate < 0f) {
            solidElevation = maxOf(targetElevation, solidElevation + elevatingRate * dt.seconds)
        }

        if (trappedForSeconds > 0f) {
            trappedForSeconds = maxOf(0f, trappedForSeconds - dt.seconds)
            if (trappedForSeconds > 0f) {
                return true
            }
            dizziness = 0f
        }

        if (dizziness > 0f) {
            dizziness = maxOf(0f, dizziness - dt.seconds / 2f)
        } else if (dizziness < 0f) {
            dizziness = minOf(0f, dizziness + dt.seconds / 2f)
        }

        if (angularSpinningSpeed != 0f) {
            position.rotate((angularSpinningSpeed * difficulty * dt.seconds).radians)
        }

        dashingTowards?.let { dashingTowards ->
            tempVec2f.set(dashingTowards).subtract(position)
            val distanceToTarget = tempVec2f.length()
            tempVec2f.setLength(dashingSpeed).scale(difficulty).scale(dt.seconds)
            if (tempVec2f.length() > distanceToTarget) {
                position.set(dashingTowards)
                this.dashingTowards = null
                isDashing = false
                toNextProgramIndex = 0f
            } else {
                position.add(tempVec2f)
            }
        }

        toNextProgramIndex -= dt.seconds * difficulty
        while (toNextProgramIndex < 0f) {
            currentProgramIndex++
            while (currentProgramIndex !in currentProgram.indices) {
                // switch program
                currentProgram = currentState.getWeightedRandomProgram()
                currentProgramIndex = 0
            }

            val currentAction = currentProgram[currentProgramIndex]
            toNextProgramIndex += currentAction.first
            currentAction.second.invoke()
        }

        if (followingPlayer) {
            tempVec2f.set(player.position).subtract(position)
            if (tempVec2f.length() > solidRadius * 2f + player.solidRadius) {
                tempVec2f.setLength(followingPlayerSpeed).scale(difficulty).scale(dt.seconds)
                position.add(tempVec2f)
            }
        }

        if (charging) {
            charge.addToTime(dt.milliseconds * chargingTimeMultiplier)
        }

        if (nextFireIn > 0f) {
            nextFireIn -= dt.seconds
            while (nextFireIn <= 0f) {
                periodicShot()
                nextFireIn += fireEvery * difficulty
            }
        }

        if (canMeleeAttack && meleeCooldown == 0f && solidElevation < 30f && position.distance(player.position) < solidRadius + player.solidRadius + meleeAttackRadius) {
            player.bumpFrom(position)
            player.damaged()
            tempVec2f.set(player.position).subtract(position)
            melee(position, tempVec2f.angleTo(NO_ROTATION).radians, false)
            meleeCooldown = 2f
        }
        return true
    }

    override fun displace(displacement: Vec2f) {
        if (solidElevation == 0f) {
            position.add(displacement)
        }
    }

    override fun renderShadow(shapeRenderer: ShapeRenderer) {
        shapeRenderer.filledCircle(
            position,
            radius = shadowRadii.x,
            color = Color.BLACK.toFloatBits()
        )
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        if (appearing || disappearing) {
            appear.render(batch, shapeRenderer)
            return
        }

        val slice = if (solidElevation == 0f) standTexture else flyTexture
        if (charging) {
            charge.position.set(position).add(0f, -solidElevation - slice.height)
            charge.render(batch, shapeRenderer)
        }

        batch.draw(
            slice = slice,
            x = position.x - slice.width / 2f,
            y = position.y - slice.height * 0.8f - solidElevation + verticalOffset,
            width = slice.width.toFloat(),
            height = slice.height.toFloat(),
            colorBits = if (damagedForSeconds > 0f) damageColor * damagedForSeconds else batch.colorBits
        )

        val starCount =
            if (trappedForSeconds > 0f) (trappedForSeconds + 1f).floorToInt() else abs(dizziness).floorToInt()
        for (i in 0 until starCount) {
            tempVec2f.set(10f, 0f)
                .rotate((starTimer * 3f + i * PI2_F / 5f * dizzinessSign).radians)
                .scale(1f, 0.5f)
                .add(-4f, -4f) // offset the middle of the texture
                .add(position) // offset into character position
            batch.draw(
                assets.texture.littleStar,
                x = tempVec2f.x,
                y = tempVec2f.y - slice.height * 0.8f - solidElevation,
                width = 10f,
                height = 10f,
            )
        }
    }

    override fun isActive(): Boolean = !appearing && !disappearing && !deactivated

    override fun startDisappearing() {
        assets.sound.disassemble.maybePlay(position)
        disappearing = true
    }

    override fun deactivate() {
        deactivated = true
    }

    fun trapped() {
        assets.sound.headSpin.maybePlay(position)
        trappedForSeconds = 5f
        meleeCooldown = 5f
        targetElevation = 0f
        elevatingRate = -400f
        angularSpinningSpeed = 0f
        fireEvery = 0f
        importantForCamera = true
        dashingTowards = null
        followingPlayer = false
        isDashing = false
        charging = false
        currentState = returnToPosition
    }

    fun damage(strong: Boolean = false) {
        /*if (health == 0f) {
            return
        }*/
        if (damagedForSeconds <= 0f) {
            assets.sound.bossHit.maybePlay(position)
            damagedForSeconds = 0.75f
            health -= /*if (strong) 0.1f else*/ 0.05f + maxOf(0f, (player.maxHearts - 3f)) * 0.01f
        }
        /*if (health <= 0f) {
            health = 0f
            // TODO: next boss
        }*/
    }

    fun applyRotation(angle: Float) {
        if (trappedForSeconds > 0f) {
            return
        }
        dizziness += angle
        if (dizziness != 0f) {
            dizzinessSign = sign(dizziness)
        }
        if (abs(dizziness) > 5f) {
            trapped()
        }
    }
}