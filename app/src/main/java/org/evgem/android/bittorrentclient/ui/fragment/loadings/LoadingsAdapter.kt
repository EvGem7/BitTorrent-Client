package org.evgem.android.bittorrentclient.ui.fragment.loadings

import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.evgem.android.bittorrentclient.R
import org.evgem.android.bittorrentclient.data.entity.Loading
import org.evgem.android.bittorrentclient.databinding.ItemLoadingBinding

class LoadingsAdapter : RecyclerView.Adapter<LoadingsViewHolder>() {
    private val loadings = ArrayList<Loading>()

    override fun onCreateViewHolder(container: ViewGroup, type: Int): LoadingsViewHolder {
        val inflater = LayoutInflater.from(container.context)
        val binding = DataBindingUtil.inflate<ItemLoadingBinding>(inflater, R.layout.item_loading, container, false)
        return LoadingsViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return loadings.size
    }

    override fun onBindViewHolder(holder: LoadingsViewHolder, pos: Int) {
        holder.bind(loadings[pos])
    }

    fun setLoadings(newLoadings: List<Loading>) {
        loadings.clear()
        loadings.addAll(newLoadings)
    }
}