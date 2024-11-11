package net.mattemade.gametemplate

import com.littlekt.createLittleKtApp

fun main() {
    createLittleKtApp {
        width = 960
        height = 540
        title = Game.TITLE
    }.start {
        Game(it, onLowPerformance = {0f}, initialZoom = 1f)
    }
}