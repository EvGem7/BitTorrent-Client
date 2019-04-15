package org.evgem.android.bittorrentclient.ui.fragment.loadings

import android.support.v7.widget.RecyclerView
import org.evgem.android.bittorrentclient.data.entity.Loading
import org.evgem.android.bittorrentclient.databinding.ItemLoadingBinding

class LoadingsViewHolder(private val loadingBinding: ItemLoadingBinding) : RecyclerView.ViewHolder(loadingBinding.root) {
    fun bind(loading: Loading) {
        loadingBinding.loading = loading
        loadingBinding.executePendingBindings()
    }
}