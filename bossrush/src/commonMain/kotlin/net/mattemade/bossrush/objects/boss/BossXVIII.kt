package net.mattemade.bossrush.objects.boss

import com.littlekt.Context
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.Vec2f
import net.mattemade.bossrush.Assets
import net.mattemade.bossrush.objects.Boss
import net.mattemade.bossrush.objects.Program
import net.mattemade.bossrush.objects.Projectile
import net.mattemade.bossrush.objects.State
import net.mattemade.bossrush.player.Player
import net.mattemade.bossrush.shader.ParticleShader
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.random.Random

class BossXVIII(
    context: Context,
    shader: ParticleShader,
    player: Player,
    assets: Assets,
    spawn: (Projectile) -> Unit,
    spawnCollectible: (Projectile) -> Unit,
    melee: (Vec2f, Float, Boolean) -> Unit,
    destroyCollectibles: (Boss) -> Unit,
    private val spawnArena: (Int) -> Unit,
    startCameraMovement: () -> Unit,
    //position: MutableVec2f,
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
    health = 2f,//2f,
    standTexture = assets.texture.bossXVIIIStand,
    flyTexture = assets.texture.bossXVIIIFly,
    projectileTexture = assets.texture.bossIProjectile,
    associatedMusic = assets.music.bossXVIII,
) {

    init {
        canSwing = false
        canMeleeAttack = false
        meleeAttackRadius = 20f
        followingPlayerSpeed = 20f
        periodicShot = ::throwBomb
    }

    override val verticalOffset: Float = -4f

    private val jumpDestroyingCollectibles: Program = listOf(
        1f to {},
        jump,
        fall,
        jump,
        fall,
        jump,
        fall,
        4f to {
            destroyCollectibles(this)
            if (Random.nextBoolean()) spawnBoulders() else spawnBombs()
        },
        0f to {
            currentState = walkingAround
        },
    )

    private val returnGroundSpinCharge: Program = listOf(
        2f to {
            dashingTowards = tempVec2f.set(position).setLength(100f).toVec2()
            dashingSpeed = 80f
            isDashing = false
            targetElevation = 0f
            elevatingRate = 20f * sign(targetElevation - solidElevation)
        },
        5f to {
            fireEvery = 0.75f
            periodicShot = if (Random.nextBoolean()) {
                ::throwBomb
            } else {
                ::throwBoulder
            }
            angularSpinningSpeed = if (Random.nextBoolean()) 1f else -1f
        },
        1f to {
            fireEvery = 0f
            angularSpinningSpeed = 0f
        },
    ) + jumpDestroyingCollectibles

    private val returnAirSpinCharge: Program = listOf(
        2f to {
            dashingTowards = tempVec2f.set(position).setLength(100f).toVec2()
            dashingSpeed = 80f*difficulty
            isDashing = false
            targetElevation = 0f
            elevatingRate = 20f * sign(targetElevation - solidElevation) * difficulty
        },
        1f to ::floatUp,
        5f to {
            fireEvery = 0.75f
            periodicShot = ::strongAttack
            angularSpinningSpeed = if (Random.nextBoolean()) 1f else -1f
        },
        1f to {
            fireEvery = 0f
            angularSpinningSpeed = 0f
        },
    ) + jumpDestroyingCollectibles

    private val returnAirSpinThrottle: Program = listOf(
        2f to {
            dashingTowards = tempVec2f.set(position).setLength(100f).toVec2()
            dashingSpeed = 80f*difficulty
            isDashing = false
            targetElevation = 0f
            elevatingRate = 20f * sign(targetElevation - solidElevation) * difficulty
        },
        1f to ::floatUp,
        5f to {
            fireEvery = 1f
            var type = 0
            periodicShot = {
                when (type) {
                    0 -> strongAttack()
                    1 -> throwBomb()
                    2 -> throwBoulder()
                }
                type = (type + 1) % 3
            }
            angularSpinningSpeed = if (Random.nextBoolean()) 1f else -1f
        },
        1f to {
            fireEvery = 0f
            angularSpinningSpeed = 0f
        },
    ) + jumpDestroyingCollectibles


    override val returnToPosition: State = listOf(
        2f to returnGroundSpinCharge,
        1f to returnAirSpinCharge,
        0.25f to returnAirSpinThrottle,
    )

    private val startSequence: State =
        listOf(
            1f to listOf(
                0f to { currentState = walkingAround }
            )
        )

    private val commonPatterns = listOf(
        1f to listOf(
            3f to ::followPlayer,
            0f to ::stopFollowing,
            1f to {},
            1f to ::throwBoulder,
            1f to ::throwBoulder,
            1f to ::throwBoulder,
            1f to {},
        ),
        1f to listOf(
            3f to ::followPlayer,
            0f to ::stopFollowing,
            1f to {},
            1f to ::throwBomb,
            1f to ::throwBomb,
            1f to ::throwBomb,
            1f to {},
        ),
        1f to listOf(
            3f to ::followPlayer,
            0f to ::stopFollowing,
            1f to {},
            1f to ::shotgun,
            1f to ::shotgun,
            1f to ::shotgun,
            1f to {},
        ),
        1f to listOf(
            3f to ::followPlayer,
            0f to ::stopFollowing,
            1f to {},
            1f to ::simpleAttack,
            1f to ::simpleAttack,
            1f to ::strongAttack,
            1f to {},
        ),
        0.1f to returnGroundSpinCharge,
        0.1f to returnAirSpinCharge,
        0.1f to returnAirSpinThrottle,
    ).toTypedArray()

    private val flyingAround: State = listOf(
        *commonPatterns,
        0.25f to listOf(
            2f to ::startCharging,
            1f to ::stopCharging,
            2f to {
                targetElevation = 200f
                elevatingRate = 80f
            },
            8f to {
                followPlayer()
                fireEvery = 0.75f
                periodicShot = {
                    makeBomb(200f, 0f, MutableVec2f(0f, 0f))
                    spawnRandomFloatingBomb(400f, 0f)
                    spawnRandomFloatingBomb(400f, 0f)
                    spawnRandomFloatingBomb(400f, 0f)
                }
            },
            2f to {
                fireEvery = 0f
                stopFollowing()
            },
            1f to {
                targetElevation = 0f
                elevatingRate = -400f

            },
            4f to ::spawnBombs
        ),
        0.25f to listOf(
            1f to ::floatDown,
            0f to {
                currentState = walkingAround
            }
        )
    )

    private val walkingAround: State =
        listOf(
            *commonPatterns,
            0.25f to listOf(
                10f to {
                    dashingTowards = tempVec2f.set(0f, 0f).toVec2()
                    dashingSpeed = 40f*difficulty
                },
                2f to ::startCharging,
                1f to ::stopCharging,
                10f to {
                    var extraAngle = 0f
                    fireEvery = 0.1f
                    periodicShot = {
                        fireProjectiles(
                            count = (6 - health * 2).roundToInt(),
                            PI2_F,
                            position.toVec2(),
                            40f,
                            elevation = 16f,
                            elevationRateOverride = -4f,
                            isReversible = false,
                            spawnsCollectible = false,
                            texture = assets.texture.dangerousProjectile,
                            scale = 0.5f,
                            tracking = false,
                            extraAngle = extraAngle
                        )
                        extraAngle += fireEvery / 2f
                    }
                },
                2f to {
                    fireEvery = 0f
                }
            ),
            0.25f to listOf(
                1f to ::floatUp,
                0f to {
                    currentState = flyingAround
                }
            )
        )

    private var sentSignal1 = false
    private var sentSignal2 = false
    private var sentSignal3 = false
    private var currentSignal = {}

    private val switchingArena: State = listOf(
        1f to listOf(
            2f to {
                trappedForSeconds = 0f
                fireEvery = 0f
                angularSpinningSpeed = 0f
                followingPlayer = false
                isDashing = false
                charging = false
                dashingTowards = Vec2f(0f, -100f)
                dashingSpeed = 120f
                targetElevation = 0f
                elevatingRate = -400f
            },
            3f to {
                currentSignal()
            },
            0f to {
                currentState = returnToPosition
            }
        )
    )

    override fun preUpdate() {
        if (health < 0.5f) {
            difficulty = 1f + 3f * (0.5f - maxOf(0f, health))
            if (!sentSignal3) {
                health = 0.5f
                sentSignal3 = true
                currentSignal = { spawnArena(1) }
                currentState = switchingArena
            }
        } else if (health < 1f) {
            if (!sentSignal2) {
                health = 1f
                sentSignal2 = true
                canSwing = true
                currentSignal = { spawnArena(2) }
                currentState = switchingArena
            }
        } else if (health < 1.5f) {
            if (!sentSignal1) {
                health = 1.5f
                sentSignal1 = true
                canMeleeAttack = true
                currentSignal = { spawnArena(3) }
                currentState = switchingArena
            }
        }
    }

    init {
        currentState = startSequence
    }
}