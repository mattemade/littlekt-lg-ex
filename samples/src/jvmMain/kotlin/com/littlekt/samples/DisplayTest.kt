package com.littlekt.samples

import com.littlekt.createLittleKtApp
import com.littlekt.graphics.Color

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
fun main(args: Array<String>) {
    createLittleKtApp {
        width = 960
        height = 540
        vSync = true
        title = "JVM - Display Test"
        icons = listOf("icon_16x16.png", "icon_32x32.png", "icon_48x48.png")
        backgroundColor = Color.DARK_GRAY
    }.start {
        DisplayTest(it)
    }
}