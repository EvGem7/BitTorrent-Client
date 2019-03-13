package org.evgem.android.bittorrentclient.util

import android.databinding.BindingAdapter
import com.github.lzyzsd.circleprogress.DonutProgress

@BindingAdapter("bind:donut_progress")
fun setDonutProgress(donutProgress: DonutProgress, progress: Float) {
    donutProgress.progress = progress
}