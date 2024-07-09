package com.littlekt.graphics

import com.littlekt.Context

/**
 * @author Colton Daily
 * @date 11/23/2021
 */
interface Preparable {
    val prepared: Boolean

    /**
     * Prepares/builds this object to be used by GL. Do any generating, uploading, etc of data here
     * **Only needs to be called ONCE per object!!**
     */
    fun prepare(context: Context)
}