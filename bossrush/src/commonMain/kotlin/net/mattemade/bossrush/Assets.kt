package net.mattemade.bossrush

import com.littlekt.Context
import com.littlekt.PreparableGameAsset
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.g2d.TextureSlice
import net.mattemade.utils.asset.AssetPack
import net.mattemade.utils.atlas.RuntimeTextureAtlasPacker

class Assets(context: Context) : AssetPack(context) {
    private val runtimeTextureAtlasPacker = RuntimeTextureAtlasPacker(context).releasing()

    val texture by pack { Textures(context, runtimeTextureAtlasPacker) }

    private val atlas by prepare(1) { runtimeTextureAtlasPacker.packAtlas() }
}

class Textures(context: Context, private val packer: RuntimeTextureAtlasPacker): AssetPack(context) {
    private fun String.pack(): PreparableGameAsset<TextureSlice> =
        preparePlain { packer.pack(this).await() }

    val swingLight by "texture/swing_light.png".pack()
    val swingStrong by "texture/swing_strong.png".pack()

    val robot by "texture/character/mc_bot_sprites-static.png".pack()


    val arena by "texture/arena.png".pack()
    val trap by "texture/trap.png".pack()
    val trapActivated by "texture/trap_activated.png".pack()
    val littleStar by "texture/little_star.png".pack()
    val column by "texture/column.png".pack()
    val boulder by "texture/boulder.png".pack()
    val projectile by "texture/projectile.png".pack()

    val heartFilled by "texture/hud/heart_filled.png".pack()
    val heartEmpty by "texture/hud/heart_empty.png".pack()

    val resource by "texture/hud/resource.png".pack()
    val selector by "texture/hud/selector.png".pack()

    val mock by "texture/hud/mock.png".pack()
    val xviii by prepare { context.vfs["texture/xviii.png"].readTexture() }

    val cogwheelTurn0 by "texture/cogwheel/turn_0.png".pack()
    val cogwheelTurn1 by "texture/cogwheel/turn_1.png".pack()
    val cogwheelTurn2 by "texture/cogwheel/turn_2.png".pack()

    val spikeBall by "texture/spikeball.png".pack()

    val bossStand by "texture/i/stand.png".pack()
    val bossFly by "texture/i/fly.png".pack()

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