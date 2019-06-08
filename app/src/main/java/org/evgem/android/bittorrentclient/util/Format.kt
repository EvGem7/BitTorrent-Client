package org.evgem.android.bittorrentclient.util

private val SPEED_POSTFIXES = arrayOf("B/s", "KiB/s", "MiB/s", "GiB/s", "TiB/s", "PiB/s", "EiB/s", "ZiB/s", "YiB/s")

fun getFormattedTime(seconds: Long): String {
    val minutes = seconds / 60
    val hours = minutes / 60
    return buildString {
        if (hours > 0) {
            append(hours)
            append("h ")
        }
        if (minutes % 60 > 0) {
            append(minutes % 60)
            append("m ")
        }
        if (seconds % 60 > 0) {
            append(seconds % 60)
            append('s')
        }
    }
}

fun getFormattedSpeed(bytesPerSecond: Long): String {
    var speed = bytesPerSecond.toDouble()
    var i = 0
    while (speed > 1024.0 && i < SPEED_POSTFIXES.size) {
        speed /= 1024.0
        ++i
    }
    return String.format("%.2f %s", speed, SPEED_POSTFIXES[i])
}