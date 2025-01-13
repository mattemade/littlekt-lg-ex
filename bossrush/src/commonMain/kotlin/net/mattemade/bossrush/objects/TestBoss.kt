package net.mattemade.bossrush.objects

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.toFloatBits
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.degrees
import com.littlekt.math.geom.radians
import com.littlekt.math.geom.times
import com.littlekt.util.fastForEach
import com.littlekt.util.seconds
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.NO_ROTATION
import net.mattemade.bossrush.player.Player
import kotlin.random.Random
import kotlin.time.Duration

typealias Program = List<Pair<Float, () -> Unit>>
typealias State = List<Pair<Float, Program>>

class TestBoss(
    private val player: Player,
    private val assets: Assets,
    private val spawn: (Projectile) -> Unit,
    private val spawnCollectible: (Projectile) -> Unit,
    private val melee: (angle: Float, clockwise: Boolean) -> Unit,
) : TemporaryDepthRenderableObject {

    var health: Float = 1f
    private var damagedForSeconds: Float = 0f
    override val position = MutableVec2f(0f, -100f)
    private var trappedForSeconds = 0f
    private var meleeCooldown = 0f
    private val shadowRadii = Vec2f(10f, 5f)
    private val damageColor = Color.RED.toFloatBits()
    var elevation: Float = 0f

    override val solidRadius: Float
        get() = 8f
    private val tempVec2f = MutableVec2f()

    // program is a list of actions to choose from: "relative chance" to ("cooldown" to "action")
    private val stayingUpState: State =
        listOf(
            1f to listOf(
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

    private fun spinClockwise() { angularSpinningSpeed = 0.5f }

    private fun spinCounterClockwise() { angularSpinningSpeed = -0.5f }

    private fun stopSpinning() { angularSpinningSpeed = 0f }

    private fun simpleAttack() = fireProjectiles(1, 0f)

    private fun strongAttack() = fireProjectiles(5, 90f)

    private fun fireProjectiles(count: Int, angle: Float) {
        val deltaAngle = (angle / count).degrees
        val startAngle = (-count / 2).toFloat() * deltaAngle

        val speed = 80f
        val distance = tempVec2f.set(player.position).subtract(position).length()
        val reachingInSeconds = distance / speed
        tempVec2f.setLength(speed).rotate(startAngle)
        for (i in 0 until count) {
            spawn(
                Projectile(
                    position = position.toMutableVec2(),
                    direction = MutableVec2f(tempVec2f),
                    elevation = elevation,
                    elevationRate = -elevation/reachingInSeconds * 0.85f, // to make them fly a bit longer,
                    spawnCollectible = spawnCollectible,
                )
            )
            tempVec2f.rotate(deltaAngle)
        }
    }

    override fun update(dt: Duration): Boolean {
        if (damagedForSeconds > 0f) {
            damagedForSeconds = maxOf(0f, damagedForSeconds - dt.seconds)
        }
        if (meleeCooldown > 0f) {
            meleeCooldown = maxOf(0f, meleeCooldown - dt.seconds)
        }

        if (elevation < targetElevation && elevatingRate > 0f) {
            elevation = minOf(targetElevation, elevation + elevatingRate * dt.seconds)
        } else if (elevation> targetElevation && elevatingRate < 0f) {
            elevation = maxOf(targetElevation, elevation + elevatingRate * dt.seconds)
        }

        if (trappedForSeconds > 0f) {
            trappedForSeconds = maxOf(0f, trappedForSeconds - dt.seconds)
            if (trappedForSeconds > 0f) {
                return true
            }
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

        if (meleeCooldown == 0f && elevation < 30f && position.distance(player.position) < solidRadius + player.solidRadius + 40f) {
            player.damaged()
            player.bump(position)
            tempVec2f.set(player.position).subtract(position)
            melee(tempVec2f.angleTo(NO_ROTATION).radians, false)
            meleeCooldown = 2f
        }
        return true
    }

    override fun displace(displacement: Vec2f) {
        if (elevation == 0f) {
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
        shapeRenderer.filledCircle(
            x = position.x,
            y = position.y - 10f - elevation,
            radius = 10f,
            color = if (damagedForSeconds > 0f) damageColor * damagedForSeconds else Color.YELLOW.toFloatBits()
        )

        if (trappedForSeconds > 0f) {
            for (i in 0..2) {
                tempVec2f.set(10f, 0f)
                    .rotate((trappedForSeconds * 3f + i * PI2_F / 3f).radians)
                    .scale(1f, 0.5f)
                    .add(-4f, -4f) // offset the middle of the texture
                    .add(position) // offset into character position
                batch.draw(
                    assets.texture.littleStar,
                    x = tempVec2f.x,
                    y = tempVec2f.y - 30f - elevation,
                    width = 8f,
                    height = 8f,
                )
            }
        }
    }

    fun trapped() {
        trappedForSeconds = 5f
        targetElevation = 0f
        elevatingRate = -60f
    }

    fun damaged(strong: Boolean = false) {
        if (health == 0f) {
            return
        }
        damagedForSeconds = 0.75f
        health -= if (strong) 0.1f else 0.05f
        if (health <= 0f) {
            health = 0f
            // TODO: next boss
        }
    }
}