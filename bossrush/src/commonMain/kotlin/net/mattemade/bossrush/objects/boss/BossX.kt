package net.mattemade.bossrush.objects.boss

import com.littlekt.Context
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.PI_F
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.radians
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.NO_ROTATION
import net.mattemade.bossrush.objects.Boss
import net.mattemade.bossrush.objects.Projectile
import net.mattemade.bossrush.objects.State
import net.mattemade.bossrush.player.Player
import net.mattemade.bossrush.shader.ParticleShader
import kotlin.random.Random

private val initialHealth = 0.689f

class BossX(
    context: Context,
    shader: ParticleShader,
    player: Player,
    assets: Assets,
    spawn: (Projectile) -> Unit,
    spawnCollectible: (Projectile) -> Unit,
    melee: (Vec2f, Float, Boolean) -> Unit,
    destroyCollectibles: (Boss) -> Unit,
    startCameraMovement: () -> Unit,
    placeTrap: (MutableVec2f) -> Unit,
    placeBall: (MutableVec2f) -> Unit,
    swing: (
        from: MutableVec2f,
        angle: Float,
        elevation: Float,
        clockwise: Boolean,
        powerful: Boolean,
        powerfulAngularSpeedScale: Float,
        getTarget: ((Projectile) -> Pair<() -> Vec2f, () -> Float>?)?
    ) -> Unit,
) : Boss(
    context,
    shader,
    player,
    assets,
    spawn,
    spawnCollectible,
    melee,
    destroyCollectibles,
    position = MutableVec2f(0f, -100f),
    health = initialHealth,
    standTexture =  assets.texture.robot.slice(sliceWidth = assets.texture.robot.width / 8, sliceHeight = assets.texture.robot.height)[0][0],
    flyTexture = assets.texture.robot.slice(sliceWidth = assets.texture.robot.width / 8, sliceHeight = assets.texture.robot.height)[0][0],
    projectileTexture = assets.texture.ball,
) {

    init {
        canMeleeAttack = true
        canSwing = true
        meleeAttackRadius = 25f
    }

    override val returnToPosition: State = listOf(
        1f to listOf(
            0f to {
                currentState = walkAround
            },
        )
    )

    private val startSequence: State =
        listOf(
            1f to listOf(
                //2f to {},
                0f to { currentState = walkAround }
            )
        )


    private val walkAround: State =
        listOf(
            3f to listOf(
                3f to ::followPlayer,
            ),
            3f to listOf(
                0f to ::stopFollowing,
            ),
            3f to listOf(
                1f to {
                    canSwing = false
                    canMeleeAttack = false
                    chargingTimeMultiplier = 2f
                    startCharging()
                },
                0.5f to {
                    stopCharging()
                    tempVec2f.set(position).subtract(player.position).setLength(32f).rotate((PI2_F / 3f).radians)
                        .add(position)
                    placeBall(tempVec2f.toMutableVec2())
                },
                0.5f to {
                    val angle = tempVec2f.set(position).subtract(player.position)
                        .angleTo(NO_ROTATION).radians + PI_F/3f
                    swing(position, angle, solidElevation, false, true, 2f) {
                        player::position to { 16f }
                    }
                },
                1.5f to {
                    canSwing = true
                    canMeleeAttack = true
                }
            ),

            1f to listOf(
                2f to {
                    canSwing = false
                    canMeleeAttack = false
                    chargingTimeMultiplier = 1f
                    startCharging()
                },
                0.5f to {
                    stopCharging()
                    tempVec2f.set(position).subtract(player.position).setLength(32f).rotate((-PI_F / 2f).radians).add(position)
                    placeTrap(tempVec2f.toMutableVec2())
                },
                1f to {
                    canSwing = true
                    canMeleeAttack = true
                }
            ),

            0.25f to listOf(
                3f to {
                    canSwing = false
                    canMeleeAttack = false
                    chargingTimeMultiplier = 2/3f
                    startCharging()
                },
                0.5f to {
                    stopCharging()
                    health = minOf(initialHealth, health + initialHealth * 0.2f)
                },
                1f to {
                    canSwing = true
                    canMeleeAttack = true
                }
            ),
        )

    private fun randomSpin() {
        if (Random.nextBoolean()) spinCounterClockwise() else spinClockwise()
    }

    init {
        currentState = startSequence
    }
}