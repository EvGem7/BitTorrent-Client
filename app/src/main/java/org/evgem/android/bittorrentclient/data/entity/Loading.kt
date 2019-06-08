package org.evgem.android.bittorrentclient.data.entity

import org.evgem.android.bittorrentclient.data.business.LoadingController
import org.evgem.android.bittorrentclient.service.LoadingService
import java.math.BigDecimal
import java.math.MathContext

/**
 * Entity class for data binding.
 */
data class Loading(
    val name: String,
    val status: String,
    var progress: Float,
    val downSpeed: String,
    val upSpeed: String,
    val size: String,
    val eta: String,
    val controller: LoadingService.ControllerWrapper
) {
    init {
        progress = BigDecimal(progress.toDouble()).round(MathContext(3)).toFloat()
    }
}