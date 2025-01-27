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
    val bombProjectile by "texture/bomb_projectile.png".pack()

    val heart by "texture/heart.png".pack()
    val heartFilled by "texture/hud/heart_filled.png".pack()
    val heartEmpty by "texture/hud/heart_empty.png".pack()

    val resource by "texture/hud/resource.png".pack()
    val selector by "texture/hud/selector.png".pack()

    val mock by "texture/hud/mock.png".pack()
    val xviii by prepare { context.vfs["texture/xviii.png"].readTexture() }

    val cogwheel by "texture/cogwheel.png".pack()

    val spikeBall by "texture/spikeball.png".pack()

    val bossIStand by "texture/boss_1_land.png".pack()
    val bossIFly by "texture/boss_1_fly.png".pack()
    val bossIProjectile by "texture/boss_1_projectile.png".pack()

    val bossIIStand by "texture/i/stand.png".pack()
    val bossIIFly by "texture/i/fly.png".pack()

    val giantClock by "texture/giant_clock.png".pack()
    val giantClockMinute by "texture/giant_clock_minute.png".pack()
    val giantClockHour by "texture/giant_clock_hour.png".pack()
    val giantClockI by "texture/giant_clock_i.png".pack()
    val giantClockII by "texture/giant_clock_ii.png".pack()
    val giantClockV by "texture/giant_clock_v.png".pack()
    val giantClockX by "texture/giant_clock_x.png".pack()

    val outro1 by prepare { context.vfs["texture/outro/1.png"].readTexture() }
    val outro2 by prepare { context.vfs["texture/outro/2.png"].readTexture() }
    val outro3 by prepare { context.vfs["texture/outro/3.png"].readTexture() }
    val outro4 by prepare { context.vfs["texture/outro/4.png"].readTexture() }
}

class Sound(context:Context): AssetPack(context) {
    val lightSwing by pack {
        SoundPack(context, listOf(
            "sound/Light Hit 1.wav",
            "sound/Light Hit 2.wav",
            "sound/Light hit 3.wav",
            "sound/Light hit 4.wav",
        ))
    }
    val strongSwing by pack {
        SoundPack(context, listOf(
            "sound/Heavy Hit 1.wav",
            "sound/Heavy Hit 2.wav",
        ))
    }

    val arenaRotating by prepare {
        context.resourcesVfs["sound/Floor Rotating.wav"].readAudioStreamEx()
    }

    val bossFire by prepare {
        context.resourcesVfs["sound/Boss Firing.wav"].readAudioClipEx()
    }

    val bossHit by prepare {
        context.resourcesVfs["sound/Boss gets hit.wav"].readAudioClipEx()
    }

    val playerHit by prepare {
        context.resourcesVfs["sound/Player gets hit.mp3"].readAudioClipEx()
    }

    val placeBall by prepare {
        context.resourcesVfs["sound/Placing Ball.wav"].readAudioClipEx()
    }

    val projectileLand by prepare {
        context.resourcesVfs["sound/Ball hits wall or floor.mp3"].readAudioClipEx()
    }


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