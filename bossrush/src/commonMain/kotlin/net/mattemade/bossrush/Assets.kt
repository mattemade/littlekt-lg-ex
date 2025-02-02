package net.mattemade.bossrush

import co.touchlab.stately.collections.ConcurrentMutableList
import com.littlekt.Context
import com.littlekt.PreparableGameAsset
import com.littlekt.audio.AudioClipEx
import com.littlekt.file.vfs.readAudioClipEx
import com.littlekt.file.vfs.readAudioStreamEx
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.g2d.TextureSlice
import net.mattemade.utils.asset.AssetPack
import net.mattemade.utils.atlas.RuntimeTextureAtlasPacker
import kotlin.random.Random

class Assets(context: Context) : AssetPack(context) {
    private val runtimeTextureAtlasPacker = RuntimeTextureAtlasPacker(context).releasing()

    val texture by pack { Textures(context, runtimeTextureAtlasPacker) }
    val sound by pack { Sound(context) }
    val music by pack { Music(context) }

    private val atlas by prepare(1) { runtimeTextureAtlasPacker.packAtlas() }
}

class Textures(context: Context, private val packer: RuntimeTextureAtlasPacker): AssetPack(context) {
    private fun String.pack(): PreparableGameAsset<TextureSlice> =
        preparePlain { packer.pack(this).await() }

    val whitePixel by "texture/white_pixel.png".pack()

    val swingLight by "texture/swing_light.png".pack()
    val swingStrong by "texture/swing_strong.png".pack()

    val robot by "texture/mc_bot_sprites-static.png".pack()


    val arena by "texture/arena.png".pack()
    val ball by "texture/ball.png".pack()
    val trap by "texture/trap.png".pack()
    val trapActivated by "texture/trap_trigger.png".pack()
    val littleStar by "texture/star.png".pack()
    val column by "texture/column.png".pack()
    val boulder by "texture/boulder.png".pack()
    val bomb by "texture/bomb.png".pack()
    val projectile by "texture/projectile.png".pack()
    val dangerousProjectile by "texture/dangerous_projectile.png".pack()
    val bombParticles by "texture/explosion_particles.png".pack()
    val bullet by "texture/bullet.png".pack()
    val stone by "texture/stone.png".pack()
    val spinningBall by "texture/spinning_ball.png".pack()
    val collectible by "texture/collectible.png".pack()

    val heart by "texture/heart.png".pack()
    val resource by "texture/collectible.png".pack()
    val ballIcon by "texture/hud_ball_icon.png".pack()
    val trapIcon by "texture/hud_trap_icon.png".pack()
    val healIcon by "texture/hud_heal_icon.png".pack()

    val xviii by prepare { context.vfs["texture/xviii.png"].readTexture() }

    val cogwheel by "texture/cogwheel.png".pack()

    val spikeBall by "texture/spikeball.png".pack()

    val bossIStand by "texture/boss_1_land.png".pack()
    val bossIFly by "texture/boss_1_fly.png".pack()
    val bossIProjectile by "texture/boss_1_projectile.png".pack()

    val bossIIStand by "texture/boss_2_land.png".pack()
    val bossIIFly by "texture/boss_2_jump.png".pack()
    val bossV1 by "texture/boss_3_1_land.png".pack()
    val bossV2 by "texture/boss_3_2_land.png".pack()
    val bossXVIIIStand by "texture/boss_4_land.png".pack()
    val bossXVIIIFly by "texture/boss_4_fly.png".pack()


    val giantClock by "texture/giant_clock_face.png".pack()
    val giantClockMinute by "texture/giant_clock_minute_hand.png".pack()
    val giantClockHour by "texture/giant_clock_hour_hand.png".pack()
    val giantClockI by "texture/giant_clock_i.png".pack()
    val giantClockII by "texture/giant_clock_ii.png".pack()
    val giantClockV by "texture/giant_clock_v.png".pack()
    val giantClockX by "texture/giant_clock_x.png".pack()

    val outro1 by prepare { context.vfs["texture/outro/1.png"].readTexture() }
    val outro2 by prepare { context.vfs["texture/outro/2.png"].readTexture() }
    val outro3 by prepare { context.vfs["texture/outro/3.png"].readTexture() }
    val outro4 by prepare { context.vfs["texture/outro/4.png"].readTexture() }

    val menuContinue by prepare { context.vfs["texture/menu_continue.png"].readTexture() }
    val menuControls by prepare { context.vfs["texture/menu_controls.png"].readTexture() }
    val menuVolume by prepare { context.vfs["texture/menu_volume.png"].readTexture() }
    val submenuControlsManual by prepare { context.vfs["texture/submenu_manual_controls.png"].readTexture() }
    val submenuControlsAutomatic by prepare { context.vfs["texture/submenu_automatic_controls.png"].readTexture() }
    val submenuControlsSens by prepare { context.vfs["texture/submenu_mouse_sensitivity.png"].readTexture() }
}

class Sound(context:Context): AssetPack(context) {
    val lightSwing by pack {
        SoundPack(context, listOf(
            "sound/light_swing.wav",
        ))
    }
    val strongSwing by pack {
        SoundPack(context, listOf(
            "sound/strong_swing.wav",
        ))
    }

    val arenaRotating by prepare {
        context.resourcesVfs["sound/arena_rotating.wav"].readAudioStreamEx()
    }

    val bossFire by prepare {
        context.resourcesVfs["sound/Boss Firing.wav"].readAudioClipEx()
    }

    val bossHit by prepare {
        context.resourcesVfs["sound/boss_damage.wav"].readAudioClipEx()
    }

    val bossJump by prepare {
        context.resourcesVfs["sound/boss_jump.wav"].readAudioClipEx()
    }

    val bossLand by prepare {
        context.resourcesVfs["sound/boss_land.wav"].readAudioClipEx()
    }

    val boulderThrow by prepare {
        context.resourcesVfs["sound/boss_boulder_throw.wav"].readAudioClipEx()
    }

    val boulderLand by prepare {
        context.resourcesVfs["sound/boulder_land.wav"].readAudioClipEx()
    }

    val bombExplosion by prepare {
        context.resourcesVfs["sound/bomb_explosion.wav"].readAudioClipEx()
    }

    val playerHit by prepare {
        context.resourcesVfs["sound/player_damage.wav"].readAudioClipEx()
    }

    val collectBall by prepare {
        context.resourcesVfs["sound/ball_collected.wav"].readAudioClipEx()
    }

    val placeBall by prepare {
        context.resourcesVfs["sound/Placing Ball.wav"].readAudioClipEx()
    }

    val putTrap by prepare {
        context.resourcesVfs["sound/put_trap.wav"].readAudioClipEx()
    }

    val trapTrigger by prepare {
        context.resourcesVfs["sound/trap_trigger.wav"].readAudioClipEx()
    }

    val projectileLand by prepare {
        context.resourcesVfs["sound/Ball hits wall or floor.mp3"].readAudioClipEx()
    }

    val cogwheelTurn by prepare {
        context.resourcesVfs["sound/cogwheel_turn.wav"].readAudioClipEx()
    }

    val silence by prepare {
        context.resourcesVfs["sound/silence.wav"].readAudioClipEx()
    }
}

class Music(context: Context): AssetPack(context) {
    val bossI by prepare {
        context.resourcesVfs["music/5. Boss 1.mp3"].readAudioStreamEx()
    }
    val bossII by prepare {
        context.resourcesVfs["music/3. Subway.mp3"].readAudioStreamEx()
    }
    val bossV by prepare {
        context.resourcesVfs["music/8. Boss 2.mp3"].readAudioStreamEx()
    }
    val bossX by prepare {
        context.resourcesVfs["music/9. Neon Towers.mp3"].readAudioStreamEx()
    }
    val bossXVIII by prepare {
        context.resourcesVfs["music/12. Final Boss.mp3"].readAudioStreamEx()
    }

    /*I - boss 1
II - subway
V - boss 2
X - neon towers
XVIII - last boss*/
}


class SoundPack(context: Context, val fileNames: List<String>, val randomize: Boolean = true) :
    AssetPack(context) {

    private val size = fileNames.size
    private var nextIndex = 0
        get() {
            val currentValue = field
            field = (currentValue + 1) % size
            return currentValue
        }

    val sound: AudioClipEx
        get() = concurrentSounds.get(if (randomize) Random.nextInt(size) else nextIndex)

    private val concurrentSounds = ConcurrentMutableList<AudioClipEx>()

    init {
        fileNames.forEach {
            prepare {
                context.resourcesVfs[it].readAudioClipEx().also { concurrentSounds.add(it) }
            }
        }
    }
}