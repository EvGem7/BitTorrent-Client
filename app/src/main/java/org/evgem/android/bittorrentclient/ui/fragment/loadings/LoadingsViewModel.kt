package org.evgem.android.bittorrentclient.ui.fragment.loadings

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.evgem.android.bittorrentclient.App
import org.evgem.android.bittorrentclient.data.entity.Loading

class LoadingsViewModel : ViewModel(), App.TestObserver {
    val loadings: MutableLiveData<List<Loading>> = MutableLiveData()

    //not main thread
    override fun observe(list: List<Loading>) {
        loadings.postValue(list)
    }

    init {
        App.addObserver(this)
    }
}