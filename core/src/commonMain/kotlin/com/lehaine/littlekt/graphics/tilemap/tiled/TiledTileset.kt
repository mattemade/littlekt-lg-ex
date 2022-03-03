package com.lehaine.littlekt.graphics.tilemap.tiled

import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.graphics.tilemap.TileSet
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
class TiledTileset(
    val tileWidth: Int,
    val tileHeight: Int,
    val tiles: List<Tile>
) : TileSet {

    data class Tile(
        val slice: TextureSlice,
        val id: Int,
        val width: Int,
        val height: Int,
        val offsetX: Int,
        val offsetY: Int,
        val frames: List<AnimatedTile>,
        val properties: Map<String, TiledMap.Property>
    )

    data class AnimatedTile(
        val slice: TextureSlice,
        val id: Int,
        val duration: Duration,
        val width: Int,
        val height: Int,
        val offsetX: Int,
        val offsetY: Int,
    )
}