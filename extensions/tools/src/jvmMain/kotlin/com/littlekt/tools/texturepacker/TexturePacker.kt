package com.littlekt.tools.texturepacker

import com.littlekt.tools.FileNameComparator
import com.littlekt.tools.texturepacker.template.createAtlasPage
import com.littlekt.util.packer.Bin
import com.littlekt.util.packer.MaxRectsPacker
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 1/31/2022
 */
class TexturePacker(val config: TexturePackerConfig) {

    private val extensions = listOf("png", "jpg", "jpeg")
    private var files: Sequence<File> = sequenceOf()
    private val imageProcessor = ImageProcessor(config)
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun process() {
        files = File(config.inputDir).walkTopDown()
            .onEnter {
                !config.ignoreDirs.contains(it.invariantSeparatorsPath)
            }
            .filter {
                extensions.contains(it.extension) && !config.ignoreFiles.contains(
                    it.invariantSeparatorsPath
                )
            }
            .sortedWith(FileNameComparator())
    }

    fun pack() {
        val outputDir = File(config.outputDir)
        files.forEach { imageProcessor.addImage(it) }
        val packer = MaxRectsPacker(config.packingOptions)
        packer.add(imageProcessor.data)

        writeImages(outputDir, packer.bins)
    }

    @Suppress("UNCHECKED_CAST")
    private fun writeImages(outputDir: File, bins: List<Bin>) {
        outputDir.mkdirs()

        bins.forEachIndexed { index, bin ->
            var canvas = BufferedImage(bin.width, bin.height, BufferedImage.TYPE_INT_ARGB)
            bin.rects.forEach { rect ->
                rect as ImageRectData
                val image = rect.loadImage()
                image.copyTo(
                    rect.offsetX,
                    rect.offsetY,
                    rect.regionWidth,
                    rect.regionHeight,
                    canvas,
                    rect.x,
                    rect.y,
                    rect.isRotated,
                    config.packingOptions.extrude
                )
            }
            val imageName = if (bins.size > 1) "${config.outputName}-$index.png" else "${config.outputName}.png"
            val jsonName = if (bins.size > 1) "${config.outputName}-$index.json" else "${config.outputName}.json"
            val relatedMultiPacks = if (bins.size > 1) {
                val relatedMultiPacks = mutableListOf<String>()
                var i = 0
                while (i < index) {
                    relatedMultiPacks += "${config.outputName}-$i.json"
                    i++
                }
                i = index
                while (i < bins.lastIndex) {
                    i++
                    relatedMultiPacks += "${config.outputName}-$i.json"
                }
                relatedMultiPacks
            } else {
                listOf()
            }
            if (config.packingOptions.bleed) {
                canvas = ColorBleedEffect().processImage(canvas, config.packingOptions.bleedIterations)
            }
            val page = createAtlasPage(canvas, imageName, bin.rects as List<ImageRectData>, relatedMultiPacks, config.crop)

            ImageIO.write(canvas, "png", File(outputDir, imageName))
            val json = json.encodeToString(page)
            val jsonFile = File(outputDir, jsonName).also { it.createNewFile() }
            FileOutputStream(jsonFile).use {
                it.write(json.toByteArray())
            }
        }
    }

    private fun BufferedImage.copyTo(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        dst: BufferedImage,
        dx: Int,
        dy: Int,
        rotated: Boolean,
        extrude: Int,
    ) {
        if (rotated) {
            for (i in 0 until width) {
                for (j in 0 until height) {
                    dst.plot(dx + j + extrude, dy + width + extrude - i - 1, getRGB(x + i, y + j))
                }
            }
        } else {
            for (i in 0 until width) {
                for (j in 0 until height) {
                    dst.plot(dx + i + extrude, dy + j + extrude, getRGB(x + i, y + j))
                }
            }
        }

        if (extrude > 0) {
            extrude(dst, dx, dy, width, height, rotated, extrude)
        }
    }

    private fun BufferedImage.extrude(
        dst: BufferedImage,
        dx: Int,
        dy: Int,
        dstWidth: Int,
        dstHeight: Int,
        rotated: Boolean,
        extrude: Int
    ) {
        run top@{
            for (y in 0 until extrude) {
                for (x in 0 until min(width, dstWidth)) {
                    if (rotated) {
                        dst.plot(y + dx, dy + dstWidth - x + extrude - 1, getRGB(x, 0))
                    } else {
                        dst.plot(x + dx + extrude, y + dy, getRGB(x, 0))
                    }
                }
            }
        }

        run bottom@{
            for (i in 1 until extrude + 1) {
                for (x in 0 until min(width, dstWidth)) {
                    if (rotated) {
                        dst.plot(
                            i + dstHeight + dx + extrude - 1,
                            dy + dstWidth - x + extrude - 1,
                            getRGB(x, height - 1)
                        )
                    } else {
                        dst.plot(
                            x + dx + extrude,
                            dstHeight + dy - i + extrude * 2,
                            getRGB(x, height - 1)
                        )
                    }
                }
            }
        }

        run left@{
            for (x in 0 until extrude) {
                for (y in 0 until min(height, dstHeight)) {
                    if (rotated) {
                        dst.plot(
                            y + dx + extrude,
                            dy + extrude * 2 + dstWidth - x - 1,
                            getRGB(0, y)
                        )
                    } else {
                        dst.plot(x + dx, y + dy + extrude, getRGB(0, y))
                    }
                }
            }
        }
        run right@{
            for (i in 1 until extrude + 1) {
                for (y in 0 until min(height, dstHeight)) {
                    if (rotated) {
                        dst.plot(
                            dx - y + extrude + dstHeight - 1,
                            dy + extrude + dstWidth - i - dstWidth,
                            getRGB(width - 1, height - 1 - y)
                        )
                    } else {
                        dst.plot(
                            dstWidth + dx - i + extrude * 2,
                            y + dy + extrude,
                            getRGB(width - 1, y)
                        )
                    }
                }
            }
        }


        run topLeft@{
            for (x in 0 until extrude) {
                for (y in 0 until extrude) {
                    if (rotated) {
                        dst.plot(x + dx, y + dy, getRGB(width - 1, 0))
                    } else {
                        dst.plot(x + dx, y + dy, getRGB(0, 0))
                    }
                }
            }
        }

        run topRight@{
            for (x in extrude downTo 1) {
                for (y in 0 until extrude) {
                    if (rotated) {
                        dst.plot(
                            dx + dstHeight - x + extrude * 2,
                            y + dy,
                            getRGB(width - 1, height - 1)
                        )
                    } else {
                        dst.plot(dx + dstWidth - x + extrude * 2, y + dy, getRGB(width - 1, 0))
                    }
                }
            }
        }

        run bottomLeft@{
            for (x in 0 until extrude) {
                for (y in extrude downTo 1) {
                    if (rotated) {
                        dst.plot(x + dx, dy + dstWidth - y + extrude * 2, getRGB(0, 0))
                    } else {
                        dst.plot(x + dx, dy + dstHeight - y + extrude * 2, getRGB(0, height - 1))
                    }
                }
            }
        }

        run bottomRight@{
            for (x in extrude downTo 1) {
                for (y in extrude downTo 1) {
                    if (rotated) {
                        dst.plot(
                            dx + dstHeight - x + extrude * 2,
                            dy + dstWidth - y + extrude * 2,
                            getRGB(0, height - 1)
                        )
                    } else {
                        dst.plot(
                            dx + dstWidth - x + extrude * 2,
                            dy + dstHeight - y + extrude * 2,
                            getRGB(width - 1, height - 1)
                        )
                    }
                }
            }
        }

    }

    private fun BufferedImage.plot(x: Int, y: Int, argb: Int) {
        if (x in 0 until width && y in 0 until height) {
            setRGB(x, y, argb)
        }
    }
}

