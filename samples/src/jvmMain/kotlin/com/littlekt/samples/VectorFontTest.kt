package com.littlekt.samples

import com.littlekt.createLittleKtApp
import com.littlekt.graphics.Color

/**
 * @author Colton Daily
 * @date 10/21/2022
 */
fun main(args: Array<String>) {
    createLittleKtApp {
        width = 960
        height = 540
        vSync = true
        title = "JVM - GPU Font Test"
        backgroundColor = Color.DARK_GRAY
    }.start {
        VectorFontTest(it)
    }
}