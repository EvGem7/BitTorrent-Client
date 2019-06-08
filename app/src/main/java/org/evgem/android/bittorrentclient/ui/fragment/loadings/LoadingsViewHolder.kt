package org.evgem.android.bittorrentclient.ui.fragment.loadings

import android.support.v7.widget.RecyclerView
import android.widget.ImageButton
import org.evgem.android.bittorrentclient.R
import org.evgem.android.bittorrentclient.data.entity.Loading
import org.evgem.android.bittorrentclient.databinding.ItemLoadingBinding

class LoadingsViewHolder(private val loadingBinding: ItemLoadingBinding) : RecyclerView.ViewHolder(loadingBinding.root) {
    init {
        val resumeButton = itemView.findViewById<ImageButton>(R.id.loading_resume)
        val pauseButton = itemView.findViewById<ImageButton>(R.id.loading_pause)
        val cancelButton = itemView.findViewById<ImageButton>(R.id.loading_cancel)

        resumeButton.setOnClickListener { loadingBinding.loading?.controller?.start() }
        pauseButton.setOnClickListener { loadingBinding.loading?.controller?.stop() }
        cancelButton.setOnClickListener { loadingBinding.loading?.controller?.cancel() }
    }

    fun bind(loading: Loading) {
        loadingBinding.loading = loading
        loadingBinding.executePendingBindings()
    }
}