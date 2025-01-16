package net.mattemade.bossrush

import com.littlekt.createLittleKtApp

fun main() {
    createLittleKtApp {
        width = 426
        height = 240
        title = Game.TITLE
        vSync = true
    }.start {
        Game(it, onLowPerformance = {0f}, initialZoom = 1f)
    }
}