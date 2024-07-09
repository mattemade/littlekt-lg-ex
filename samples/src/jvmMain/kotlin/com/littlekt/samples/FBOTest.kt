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
        title = "JVM - FBO Test"
        backgroundColor = Color.DARK_GRAY
    }.start {
        FBOTest(it)
    }
}