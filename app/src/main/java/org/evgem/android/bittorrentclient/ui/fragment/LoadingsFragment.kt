package org.evgem.android.bittorrentclient.ui.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.evgem.android.bittorrentclient.R

class LoadingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        println("///////////////////////////////////////////////////////////////////////////////////////////////// ")

        return inflater.inflate(R.layout.fragment_loadings, container, false)
    }
}