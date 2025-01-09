package net.mattemade.bossrush

import com.littlekt.Context
import com.littlekt.PreparableGameAsset
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


    val arena by "texture/arena.png".pack()
    val trap by "texture/trap.png".pack()
    val trapActivated by "texture/trap_activated.png".pack()
    val littleStar by "texture/little_star.png".pack()

    //val sequence by preparePlain(1) { arrayOf(/*down,*/ downRight, right, upRight/*, up*/) }
    //val sequence2 by preparePlain(2) { arrayOf(down, downRight, right, upRight, up) }
}