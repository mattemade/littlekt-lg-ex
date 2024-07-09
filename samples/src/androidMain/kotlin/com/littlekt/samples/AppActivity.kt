package com.littlekt.samples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.LittleKtActivity
import com.littlekt.LittleKtProps
import com.littlekt.graphics.Color

/**
 * @author Colton Daily
 * @date 2/11/2022
 */
class AppActivity : LittleKtActivity() {

    override fun LittleKtProps.configureLittleKt() {
        activity = this@AppActivity
        backgroundColor = Color.DARK_GRAY
    }

    override fun createContextListener(context: Context): ContextListener {
        return FBOMultiTargetTest(context)
    }
}