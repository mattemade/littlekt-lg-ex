package net.mattemade.bossrush.objects.boss

import com.littlekt.Context
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.objects.Boss
import net.mattemade.bossrush.objects.Projectile
import net.mattemade.bossrush.objects.State
import net.mattemade.bossrush.player.Player
import net.mattemade.bossrush.shader.ParticleShader
import kotlin.random.Random

class BossV(
    context: Context,
    shader: ParticleShader,
    player: Player,
    assets: Assets,
    spawn: (Projectile) -> Unit,
    spawnCollectible: (Projectile) -> Unit,
    melee: (Vec2f, Float, Boolean) -> Unit,
    destroyCollectibles: (Boss) -> Unit,
    startCameraMovement: () -> Unit,
    position: MutableVec2f,
) : Boss(
    context,
    shader,
    player,
    assets,
    spawn,
    spawnCollectible,
    melee,
    destroyCollectibles,
    position,
    health = 0.689f / 2f,
    standTexture = assets.texture.bossIIStand,
    flyTexture = assets.texture.bossIIFly,
    projectileTexture = assets.texture.projectile,
) {

    init {
        canMeleeAttack = true
        meleeAttackRadius = 10f
        followingPlayerSpeed = 20f
        periodicShot = ::throwBomb
    }

    val jump = 0.25f to {
        elevatingRate = 200f
        targetElevation = 200f
    }
    val smallJump = 0.25f to {
        elevatingRate = 100f
        targetElevation = 200f
    }
    val fall = 0.25f to {
        elevatingRate = -200f
        targetElevation = 0f
    }
    val smallFall = 0.25f to {
        elevatingRate = -100f
        targetElevation = 0f
    }

    override val returnToPosition: State = listOf(
        1f to listOf(
            2f to {
                dashingTowards = tempVec2f.set(position).setLength(100f).toVec2()
                dashingSpeed = 80f
                isDashing = false
                //elevatingRate = 100f
                //targetElevation = 20f
                //elevatingRate = 20f * sign(targetElevation - solidElevation)
            },
            5f to {
                fireEvery = 1f
                angularSpinningSpeed = if (Random.nextBoolean()) 1f else -1f
            },
            0f to {
                fireEvery = 0f
                angularSpinningSpeed = 0f
                currentState = walkingAround
            },
        )
    )

    private val startSequence: State =
        listOf(
            1f to listOf(
                /*1f to {},
                smallJump,
                smallFall,
                0.25f to {},
                smallJump,
                smallFall,
                0.25f to {},
                smallJump,
                smallFall,
                1f to {},*/
                0f to { currentState = walkingAround }
            )
        )


    //private val followPlayer: State = listOf()

    private val walkingAround: State =
        listOf(
            1.5f to listOf(
                3f to ::followPlayer,
                0f to ::stopFollowing,
                1f to {},
                1f to ::shotgun,
                1f to ::shotgun,
                1f to ::shotgun,
                1f to {},
            ),
            0.25f to listOf(
                20f to {
                    dashingTowards = tempVec2f.set(position).setLength(100f).toVec2()
                    dashingSpeed = 40f
                },
                1f to {},
                2f to ::startCharging,
                0.1f to {
                    stopCharging()
                    targetElevation = 0.01f
                    elevatingRate = 200f
                },
                10f to ::dashIntoPlayer,
                0.1f to {
                    targetElevation = 0f
                    elevatingRate = -200f
                },
                1f to {},
            ),
            0.25f to listOf(
                0f to { currentState = returnToPosition },
            ),
            0.1f to listOf(
                20f to {
                    dashingTowards = tempVec2f.set(position).setLength(100f).toVec2()
                    dashingSpeed = 40f
                },
                1f to {},
                2f to ::startCharging,
                0f to ::stopCharging,
                5f to {
                    importantForCamera = false
                    fireEvery = 1f
                    isDashing = true
                    angularSpinningSpeed = if (Random.nextBoolean()) 5f else -5f
                    startCameraMovement()
                },
                0f to {
                    isDashing = false
                    trapped()
                    startCameraMovement()
                }
            )
        )

    private fun randomSpin() {
        if (Random.nextBoolean()) spinCounterClockwise() else spinClockwise()
    }

    init {
        currentState = startSequence
    }
}