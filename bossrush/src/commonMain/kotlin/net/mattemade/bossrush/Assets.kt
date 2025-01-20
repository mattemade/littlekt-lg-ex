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

    val up by "texture/character/up.png".pack()
    val upRight by "texture/character/up_right.png".pack()
    val right by "texture/character/right.png".pack()
    val downRight by "texture/character/down_right.png".pack()
    val down by "texture/character/down.png".pack()
    val swingLight by "texture/swing_light.png".pack()
    val swingStrong by "texture/swing_strong.png".pack()

    val robot by "texture/character/mc_bot_sprites-static.png".pack()


    val arena by "texture/arena.png".pack()
    val trap by "texture/trap.png".pack()
    val trapActivated by "texture/trap_activated.png".pack()
    val littleStar by "texture/little_star.png".pack()
    val column by "texture/column.png".pack()

    val heartFilled by "texture/hud/heart_filled.png".pack()
    val heartEmpty by "texture/hud/heart_empty.png".pack()


    val clockBg by "texture/hud/clock_bg.png".pack()
    val clockMinute by "texture/hud/clock_minute.png".pack()
    val clockHour by "texture/hud/clock_hour.png".pack()
    val resource by "texture/hud/resource.png".pack()
    val selector by "texture/hud/selector.png".pack()

    val mock by "texture/hud/mock.png".pack()
    val xviii by prepare { context.vfs["texture/xviii.png"].readTexture() }
    val title by prepare { context.vfs["texture/title.png"].readTexture() }

    val cogwheelTurn0 by "texture/cogwheel/turn_0.png".pack()
    val cogwheelTurn1 by "texture/cogwheel/turn_1.png".pack()
    val cogwheelTurn2 by "texture/cogwheel/turn_2.png".pack()

    val spikeBall by "texture/spikeball.png".pack()

    val bossStand by "texture/i/stand.png".pack()
    val bossFly by "texture/i/fly.png".pack()

    val outro1 by prepare { context.vfs["texture/outro/1.png"].readTexture() }
    val outro2 by prepare { context.vfs["texture/outro/2.png"].readTexture() }
    val outro3 by prepare { context.vfs["texture/outro/3.png"].readTexture() }
    val outro4 by prepare { context.vfs["texture/outro/4.png"].readTexture() }

    //val sequence by preparePlain(1) { arrayOf(/*down,*/ downRight, right, upRight/*, up*/) }
    //val sequence2 by preparePlain(2) { arrayOf(down, downRight, right, upRight, up) }
}