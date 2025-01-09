package net.mattemade.bossrush

import com.littlekt.createLittleKtApp

fun main() {
    createLittleKtApp {
        width = 320
        height = 240
        title = Game.TITLE
        vSync = false
    }.start {
        Game(it, onLowPerformance = {0f}, initialZoom = 1f)
    }
}