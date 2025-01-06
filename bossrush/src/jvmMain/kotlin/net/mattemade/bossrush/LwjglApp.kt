package net.mattemade.bossrush

import com.littlekt.createLittleKtApp

fun main() {
    createLittleKtApp {
        width = 2400
        height = 1600
        title = Game.TITLE
    }.start {
        Game(it, onLowPerformance = {0f}, initialZoom = 1f)
    }
}