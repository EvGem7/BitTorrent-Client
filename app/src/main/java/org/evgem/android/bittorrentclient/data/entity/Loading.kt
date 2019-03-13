package org.evgem.android.bittorrentclient.data.entity

import java.math.BigDecimal
import java.math.MathContext

data class Loading(
    val name: String,
    val status: String,
    var progress: Float,
    val downSpeed: String,
    val upSpeed: String,
    val size: String,
    val eta: String,
    val peersCount: Int,
    val seedsCount: Int
) {
    init {
        progress = BigDecimal(progress.toDouble()).round(MathContext(3)).toFloat()
    }
}