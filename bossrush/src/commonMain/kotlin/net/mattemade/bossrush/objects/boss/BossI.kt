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
import kotlin.math.sign
import kotlin.random.Random

class BossI(
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
    position = MutableVec2f(100f, 0f),
    health = 0.375f,
    standTexture = assets.texture.bossIStand,
    flyTexture = assets.texture.bossIFly,
    projectileTexture = assets.texture.bossIProjectile,
    associatedMusic = assets.music.bossI,
) {

    override val returnToPosition: State = listOf(
        1f to listOf(
            2f to {
                isDashing = false
                dashingTowards = tempVec2f.set(position).setLength(100f).toVec2()
                dashingSpeed = 80f
                targetElevation = 0.01f
                elevatingRate = 20f * sign(targetElevation - solidElevation)
            },
            1f to ::floatUp,
            1f to {},
            0f to {
                currentState = flyingAround
            },
        )
    )

    private val startSequence: State =
        listOf(
            1f to listOf(
                1f to {},
                1f to {
                    targetElevation = 0.001f
                    elevatingRate = 20f
                },
                1f to {
                    floatUp()
                },
                1f to {},
                0f to { currentState = flyingAround }
            )
        )

    private val flyingAround: State =
        listOf(
            1.5f to listOf( // stop and throw 3 balls
                1.5f to ::randomSpin,
                0.5f to ::stopSpinning,
                0.5f to ::simpleAttack,
                0.5f to ::simpleAttack,
                0.5f to ::simpleAttack,
            ),
            1.5f to listOf( // stop and throw 2 balls and 5 balls
                1.5f to ::randomSpin,
                0.5f to ::stopSpinning,
                1f to ::simpleAttack,
                1f to ::simpleAttack,
                2f to ::strongAttack,
            ),
            0.1f to listOf(
                2f to ::startCharging,
                0f to ::stopCharging,
                5f to {
                    importantForCamera = false
                    fireEvery = 0.5f
                    isDashing = true
                    angularSpinningSpeed = if (Random.nextBoolean()) 5f else -5f
                    startCameraMovement()
                },
                0f to {
                    isDashing = false
                    trapped()
                    startCameraMovement()
                }
            ),
            0.25f to listOf(
                1f to ::floatDown,
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
                1f to ::floatUp,
                1f to {},
            )
        )

    private fun randomSpin() {
        if (Random.nextBoolean()) spinCounterClockwise() else spinClockwise()
    }

    init {
        currentState = startSequence
    }
}