package com.littlekt.util

import com.littlekt.util.datastructure.DoubleArrayList
import com.littlekt.util.datastructure.FloatArrayList
import com.littlekt.util.datastructure.IntArrayList

/**
 * @author Colton Daily
 * @date 12/8/2021
 */
expect fun Double.toString(precision: Int): String

fun Float.toString(precision: Int): String = this.toDouble().toString(precision)

inline fun <T> List<T>.forEachReversed(action: (T) -> Unit): Unit {
    for (i in lastIndex downTo 0) {
        action(this[i])
    }
}

inline fun <T> List<T>.fastForEach(callback: (T) -> Unit) {
    var n = 0
    while (n < size) {
        callback(this[n++])
    }
}

inline fun <T> Array<T>.fastForEach(callback: (T) -> Unit) {
    var n = 0
    while (n < size) {
        callback(this[n++])
    }
}

inline fun IntArrayList.fastForEach(callback: (Int) -> Unit) {
    var n = 0
    while (n < size) {
        callback(this.getAt(n++))
    }
}

inline fun FloatArrayList.fastForEach(callback: (Float) -> Unit) {
    var n = 0
    while (n < size) {
        callback(this.getAt(n++))
    }
}

inline fun DoubleArrayList.fastForEach(callback: (Double) -> Unit) {
    var n = 0
    while (n < size) {
        callback(this.getAt(n++))
    }
}

inline fun <T> List<T>.fastForEachWithIndex(callback: (index: Int, value: T) -> Unit) {
    var n = 0
    while (n < size) {
        callback(n, this[n])
        n++
    }
}

inline fun <T> Array<T>.fastForEachWithIndex(callback: (index: Int, value: T) -> Unit) {
    var n = 0
    while (n < size) {
        callback(n, this[n])
        n++
    }
}

inline fun IntArrayList.fastForEachWithIndex(callback: (index: Int, value: Int) -> Unit) {
    var n = 0
    while (n < size) {
        callback(n, this.getAt(n))
        n++
    }
}

inline fun FloatArrayList.fastForEachWithIndex(callback: (index: Int, value: Float) -> Unit) {
    var n = 0
    while (n < size) {
        callback(n, this.getAt(n))
        n++
    }
}

inline fun DoubleArrayList.fastForEachWithIndex(callback: (index: Int, value: Double) -> Unit) {
    var n = 0
    while (n < size) {
        callback(n, this.getAt(n))
        n++
    }
}

inline fun <T> List<T>.fastForEachReverse(callback: (T) -> Unit) {
    var n = 0
    while (n < size) {
        callback(this[size - n - 1])
        n++
    }
}

inline fun <T> MutableList<T>.fastIterateRemove(callback: (T) -> Boolean): MutableList<T> {
    var n = 0
    var m = 0
    while (n < size) {
        if (m >= 0 && m != n) this[m] = this[n]
        if (callback(this[n])) m--
        n++
        m++
    }
    while (this.size > m) this.removeAt(this.size - 1)
    return this
}

fun <T> MutableList<T>.truncate(newSize: Int) {
    check(newSize > 0) { "'newSize' must be >= 0: $newSize" }
    if (size <= newSize) return
    subList(newSize, size).clear()
}