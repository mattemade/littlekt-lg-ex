package net.mattemade.bossrush

import com.littlekt.createLittleKtApp

fun main() {
    createLittleKtApp {
        width = 2000
        height = 1080
        title = Game.TITLE
    }.start {
        Game(it, onLowPerformance = {0f}, initialZoom = 1f)
    }
}