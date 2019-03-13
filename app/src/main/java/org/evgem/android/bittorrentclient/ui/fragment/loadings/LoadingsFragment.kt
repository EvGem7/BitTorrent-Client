package org.evgem.android.bittorrentclient.ui.fragment.loadings

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_loadings.*
import org.evgem.android.bittorrentclient.R

class LoadingsFragment : Fragment() {
    private lateinit var viewModel: LoadingsViewModel
    private val adapter = LoadingsAdapter()
    private lateinit var layoutManager: RecyclerView.LayoutManager

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        viewModel = ViewModelProviders.of(this).get(LoadingsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_loadings, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        layoutManager = LinearLayoutManager(context)
        loadings_recycler.layoutManager = layoutManager
        loadings_recycler.adapter = adapter
        loadings_recycler.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        viewModel.loadings.observe(this, Observer { loadings ->
            loadings?.let {
                println(it)
                adapter.setLoadings(it)
                adapter.notifyDataSetChanged()
            }
        })
    }
}