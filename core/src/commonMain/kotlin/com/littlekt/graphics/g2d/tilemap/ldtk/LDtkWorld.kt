package com.littlekt.graphics.g2d.tilemap.ldtk

import com.littlekt.Disposable
import com.littlekt.file.ldtk.LDtkEntityDefinition
import com.littlekt.file.ldtk.LDtkWorldLayout
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.tilemap.TileMap
import com.littlekt.util.calculateViewBounds

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkWorld(
    val worldLayout: LDtkWorldLayout,
    val backgroundColor: String,
    val levels: List<LDtkLevel>,
    val tilesets: Map<Int, LDtkTileset>,
    val enums: Map<String, LDtkEnum>,
    val entities: Map<String, LDtkEntityDefinition>
) : TileMap(), Disposable {
    val levelsMap: Map<String, LDtkLevel> by lazy { levels.associateBy { it.identifier } }

    internal var onDispose = {}

    override fun render(batch: Batch, camera: Camera, x: Float, y: Float, scale: Float) {
        viewBounds.calculateViewBounds(camera)
        levels.forEach { it.render(batch, viewBounds, it.worldX + x, it.worldY + y) }
    }

    operator fun get(level: String) = levelsMap[level] ?: error("Level: '$level' does not exist in this map!")

    override fun dispose() {
        onDispose()
    }

    override fun toString(): String {
        return "LDtkMap(levels=$levels, tilesets=$tilesets, worldLayout=$worldLayout, backgroundColor='$backgroundColor', enums=$enums)"
    }
}