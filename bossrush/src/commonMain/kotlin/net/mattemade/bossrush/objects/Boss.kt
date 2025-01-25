package net.mattemade.bossrush.objects

import com.littlekt.Context
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
import com.littlekt.math.geom.abs
import com.littlekt.math.geom.radians
import com.littlekt.math.geom.times
import com.littlekt.util.fastForEach
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.DEBUG
import net.mattemade.bossrush.NO_ROTATION
import net.mattemade.bossrush.SOUND_VOLUME
import net.mattemade.bossrush.player.Player
import net.mattemade.bossrush.shader.ParticleShader
import net.mattemade.utils.math.fill
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.random.Random
import kotlin.time.Duration

typealias Program = List<Pair<Float, () -> Unit>>
typealias State = List<Pair<Float, Program>>

class Boss(
    private val context: Context,
    private val shader: ParticleShader,
    private val player: Player,
    private val assets: Assets,
    private val spawn: (Projectile) -> Unit,
    private val spawnCollectible: (Projectile) -> Unit,
    private val melee: (position: Vec2f, angle: Float, clockwise: Boolean) -> Unit,
    private val destroyCollectibles: (Boss) -> Unit,
    override val position: MutableVec2f = MutableVec2f(0f, -100f),
    var health: Float = 1f,
) : TemporaryDepthRenderableObject {


    private val standTexture = assets.texture.bossStand
    private val flyTexture = assets.texture.bossFly
    private var damagedForSeconds: Float = 0f
    private var starTimer = 0f
    private var trappedForSeconds = 0f
    var meleeCooldown = 0f
    private val shadowRadii = MutableVec2f(0f, 0f)
    private val damageColor = Color.RED.toFloatBits()
    override var solidElevation: Float = 0f
    override val solidHeight: Float = 38f
    private var dizziness = 0f
    private var dizzinessSign = 0f

    override val solidRadius: Float
        get() = 8f
    private val tempVec2f = MutableVec2f()


    private val appearingFor = if (DEBUG) 0f else 3000f
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
                fill(x - halfWidth, y - 45f - solidElevation) // normal rendering offsets
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

    // program is a list of actions to choose from: "relative chance" to ("cooldown" to "action")
    private val stayingUpState: State =
        listOf(
            0.25f to listOf(
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
            )
        )

    private fun State.getWeightedRandomProgram(): Program {
        var sum = 0f
        this.fastForEach { sum += it.first }
        sum *= Random.nextFloat()
        for (i in indices) {
            val currentItem = this[i]
            sum -= currentItem.first
            if (sum <= 0f) {
                return currentItem.second
            }
        }
        return this.last().second
    }

    private var currentState: State = stayingUpState
    private var currentProgram: Program = currentState.getWeightedRandomProgram()
    private var currentProgramIndex = -1
    private var toNextProgramIndex = 0f

    private var angularSpinningSpeed = 0f
    private var targetElevation = 0f
    private var elevatingRate = 0f

    private fun elevate() {
        targetElevation = 20f
        elevatingRate = 20f
    }

    private fun land() {
        targetElevation = 0f
        elevatingRate = -20f
    }

    private fun spinClockwise() {
        angularSpinningSpeed = 0.5f
    }

    private fun spinCounterClockwise() {
        angularSpinningSpeed = -0.5f
    }

    private fun stopSpinning() {
        angularSpinningSpeed = 0f
    }

    private fun spawnBoulders() {
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
                        fireProjectiles(8, PI2_F, it.position, 60f, it.solidElevation + 6f, 50f, 100f, false, scale = 0.5f)
                    },
                    gravity = 100f,
                    solidRadius = 8f,
                    isReversible = false,
                )
            )
        }

    }

    private fun throwBoulder() {
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
                    fireProjectiles(8, PI2_F, it.position, 60f, it.solidElevation + 6f, 50f, 100f, false, scale = 0.5f)
                },
                gravity = 200f,
                solidRadius = 8f,
                isReversible = false,
            )
        )
    }

    private fun shotgun() = fireProjectiles(count = 8, angle = PI_F / 4f, tracking = true, speed = 120f, elevationRateOverride = 0f, scale = 0.5f, timeToLive = 0.6f)

    private fun simpleAttack() = fireProjectiles(1, 0f, tracking = true, scale = 0.5f)

    private fun strongAttack() = fireProjectiles(5, PI_F / 2f, tracking = true, scale = 0.5f)

    private fun fireProjectiles(
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
    ) {
        assets.sound.bossFire.play(volume = 20f, positionX = position.x, positionY = position.y)

        val deltaAngle = (angle / count).radians
        val startAngle = (-count / 2).toFloat() * deltaAngle

        val distance = tempVec2f.set(player.position).subtract(from).length()
        val reachingInSeconds = distance / speed
        tempVec2f.setLength(speed).rotate(if (tracking) startAngle else -tempVec2f.angleTo(NO_ROTATION))
        /*if (tracking) {
            tempVec2f.rotate(startAngle)
        }*/
        for (i in 0 until count) {
            spawn(
                Projectile(
                    assets = assets,
                    texture = assets.texture.projectile,
                    position = from.toMutableVec2(),
                    direction = MutableVec2f(tempVec2f),
                    solidElevation = elevation,
                    elevationRate = elevationRateOverride
                        ?: (-elevation / reachingInSeconds * 0.7f), // to make them fly a bit longer,
                    onSolidImpact = spawnCollectible,
                    gravity = gravity ?: 0f,
                    scale = scale,
                    timeToLive = timeToLive,
                )
            )
            tempVec2f.rotate(deltaAngle)
        }
    }

    override fun update(dt: Duration): Boolean {
        starTimer = (starTimer + dt.seconds) % PI2_F
        if (disappearing) {
            appear.update(-dt)
            disappearing = appear.liveFactor > 0f
            shadowRadii.set(10f, 5f).scale(appear.liveFactor)
            return disappearing
        } else if (appearing) {
            appearing = appear.update(dt)
            shadowRadii.set(10f, 5f)
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
            meleeCooldown = maxOf(0f, meleeCooldown - dt.seconds)
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
            position.rotate((angularSpinningSpeed * dt.seconds).radians)
        }


        toNextProgramIndex -= dt.seconds
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

        if (meleeCooldown == 0f && solidElevation < 30f && position.distance(player.position) < solidRadius + player.solidRadius + 40f) {
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
        shapeRenderer.filledEllipse(
            position,
            shadowRadii,
            innerColor = Color.BLUE.toFloatBits(),
            outerColor = Color.BLACK.toFloatBits()
        )
    }

    override fun render(batch: Batch, shapeRenderer: ShapeRenderer) {
        if (appearing || disappearing) {
            appear.render(batch, shapeRenderer)
            return
        }

        batch.draw(
            slice = if (solidElevation == 0f) standTexture else flyTexture,
            x = position.x - 16f,
            y = position.y - 45f - solidElevation,
            width = 32f,
            height = 48f,
            colorBits = if (damagedForSeconds > 0f) damageColor * damagedForSeconds else batch.colorBits
        )

        val startCount = if (trappedForSeconds > 0f) (trappedForSeconds + 1f).floorToInt() else abs(dizziness).floorToInt()
        //if (trappedForSeconds > 0f) {
            for (i in 0 until startCount) {
                tempVec2f.set(10f, 0f)
                    .rotate((starTimer * 3f + i * PI2_F / 5f * dizzinessSign).radians)
                    .scale(1f, 0.5f)
                    .add(-4f, -4f) // offset the middle of the texture
                    .add(position) // offset into character position
                batch.draw(
                    assets.texture.littleStar,
                    x = tempVec2f.x,
                    y = tempVec2f.y - 30f - solidElevation,
                    width = 8f,
                    height = 8f,
                )
            }
        //}
    }

    override fun isActive(): Boolean = !appearing && !disappearing && !deactivated

    override fun startDisappearing() {
        disappearing = true
    }

    override fun deactivate() {
        deactivated = true
    }

    fun trapped() {
        trappedForSeconds = 5f
        targetElevation = 0f
        elevatingRate = -60f
    }

    fun damaged(strong: Boolean = false) {
        /*if (health == 0f) {
            return
        }*/
        if (damagedForSeconds <= 0f) {
            assets.sound.bossHit.play(volume = SOUND_VOLUME, positionX = position.x, positionY = position.y)
            damagedForSeconds = 0.75f
            health -= if (strong) 0.1f else 0.05f
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