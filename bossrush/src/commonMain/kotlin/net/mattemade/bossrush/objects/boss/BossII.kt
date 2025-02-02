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

class BossII(
    context: Context,
    shader: ParticleShader,
    player: Player,
    assets: Assets,
    spawn: (Projectile) -> Unit,
    spawnCollectible: (Projectile) -> Unit,
    melee: (Vec2f, Float, Boolean) -> Unit,
    destroyCollectibles: (Boss) -> Unit,
    startCameraMovement: () -> Unit,
) : Boss(
    context,
    shader,
    player,
    assets,
    spawn,
    spawnCollectible,
    melee,
    destroyCollectibles,
    position = MutableVec2f(0f, 100f),
    health = 0.489f,
    standTexture = assets.texture.bossIIStand,
    flyTexture = assets.texture.bossIIFly,
    projectileTexture = assets.texture.projectile,
    associatedMusic = assets.music.bossII,
) {

    init {
        canMeleeAttack = true
        meleeAttackRadius = 20f
        periodicShot = ::throwBoulder
    }

    override val solidRadius: Float = 16f
    override val returnToPosition: State = listOf(
        1f to listOf(
            2f to {
                isDashing = false
                dashingTowards = tempVec2f.set(position).setLength(100f).toVec2()
                dashingSpeed = 80f
                elevatingRate = 100f
                targetElevation = 20f
                //elevatingRate = 20f * sign(targetElevation - solidElevation)
            },
            1f to {
                elevatingRate = -100f
                targetElevation = 0f
            },
            1f to {},
            jump,
            *fall,
            jump,
            *fall,
            jump,
            *fall,
            4f to {
                destroyCollectibles(this)
                spawnBoulders()
            },
            0f to {
                currentState = walkingAround
            },
        )
    )

    private val startSequence: State =
        listOf(
            1f to listOf(
                1f to {},
                smallJump,
                smallFall,
                0.25f to {},
                smallJump,
                smallFall,
                0.25f to {},
                smallJump,
                smallFall,
                1f to {},
                0f to { currentState = walkingAround }
            )
        )


    //private val followPlayer: State = listOf()

    private val walkingAround: State =
        listOf(
            2f to listOf(
                3f to ::followPlayer,
                0f to ::stopFollowing,
                1f to {},
                1f to ::throwBoulder,
                1f to ::throwBoulder,
                1f to ::throwBoulder,
                1f to {},
            ),
            1f to listOf(
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