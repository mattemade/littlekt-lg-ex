package com.littlekt.samples

import com.littlekt.createLittleKtApp
import com.littlekt.graphics.Color

/**
 * @author Colton Daily
 * @date 1/22/2023
 */
fun main(args: Array<String>) {
    createLittleKtApp {
        width = 960
        height = 540
        vSync = true
        title = "JVM - Scene Graph Nested Viewports Test"
        backgroundColor = Color.DARK_GRAY
    }.start {
        SceneGraphNestedViewportsTest(it)
    }
}