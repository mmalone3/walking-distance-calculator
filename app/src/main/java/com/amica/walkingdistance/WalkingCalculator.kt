package com.amica.walkingdistance

import java.util.Locale
import kotlin.math.roundToLong

data class Walk(val seconds: Double, val speed: Double) {
    val miles: Double get() = seconds / 3600.0 * speed
    val kilometers: Double get() = miles * 1.609344
}

fun formatTime(seconds: Double): String {
    val total = seconds.roundToLong()
    return String.format(Locale.US, "%dh %02dm %02ds", total / 3600, total % 3600 / 60, total % 60)
}

fun formatPace(speed: Double): String {
    if (speed <= 0.0) return "— /mi"
    val seconds = (3600.0 / speed).roundToLong()
    return String.format(Locale.US, "%d:%02d /mi", seconds / 60, seconds % 60)
}
