package org.evgem.android.bittorrentclient.ui.fragment.loadings

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import org.evgem.android.bittorrentclient.R
import java.io.FileInputStream

class LoadingsFragment : Fragment() {
    private lateinit var viewModel: LoadingsViewModel

    //views
    private lateinit var recycler: RecyclerView
    private lateinit var fab: FloatingActionButton

    //recycler stuff
    private val adapter = LoadingsAdapter()
    private lateinit var layoutManager: RecyclerView.LayoutManager

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        viewModel = ViewModelProviders.of(this).get(LoadingsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_loadings, container, false)
        recycler = v.findViewById(R.id.loadings_recycler)
        fab = v.findViewById(R.id.loadings_fab)
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //init recycler
        layoutManager = LinearLayoutManager(context)
        recycler.apply {
            layoutManager = layoutManager
            adapter = adapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        //set listeners
        fab.setOnClickListener {
            sendTorrentIntent()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode) {
            TORRENT_FILE_REQUEST_CODE -> {
                processTorrentResult(data)
            }
        }
    }

    private fun processTorrentResult(data: Intent?) {
        data?.data?.let { uri ->
            val fileDescriptor = context?.contentResolver?.openFileDescriptor(uri, "r")?.fileDescriptor ?: return
            viewModel.addNewLoading(FileInputStream(fileDescriptor))
            return
        }
        Log.e(TAG, "processTorrentResult: uri or intent is null")
    }

    private fun sendTorrentIntent() {
        //intent for choosing file
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = TORRENT_MIME_TYPE
        }
        val resolveInfo = context?.packageManager?.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo != null) {
            val chooserTitle = getString(R.string.torrent_file_chooser)
            startActivityForResult(
                Intent.createChooser(intent, chooserTitle),
                TORRENT_FILE_REQUEST_CODE
            )
        } else {
            Toast.makeText(context, R.string.no_file_manager_toast, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val TORRENT_MIME_TYPE = "application/x-bittorrent"
        private const val TORRENT_FILE_REQUEST_CODE = 1231

        private const val TAG = "LoadingsFragment"
    }
}