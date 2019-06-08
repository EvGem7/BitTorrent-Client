package org.evgem.android.bittorrentclient.util

class SpeedCalculator {
    private var firstTime = -1L
    private var lastTime = -1L
    private var firstBytes = -1L
    private var lastBytes = -1L

    val speed: Long get() {
        if (firstTime == -1L || lastTime == -1L) {
            return 0L
        }
        if (lastTime - firstTime == 0L) {
            return 0L
        }
        return (lastBytes - firstBytes) / (lastTime - firstTime) * 1000
    }

    fun put(bytes: Long) {
        if (firstTime == -1L) {
            firstTime = System.currentTimeMillis()
            firstBytes = bytes
            return
        }
        lastBytes = bytes
        lastTime = System.currentTimeMillis()
    }

    fun reset() {
        firstTime = -1L
    }
}