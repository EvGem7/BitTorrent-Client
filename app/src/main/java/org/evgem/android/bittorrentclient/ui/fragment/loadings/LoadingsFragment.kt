package org.evgem.android.bittorrentclient.ui.fragment.loadings

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.Observer
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import org.evgem.android.bittorrentclient.R
import org.evgem.android.bittorrentclient.async.StartLoadingTask
import org.evgem.android.bittorrentclient.service.LoadingService
import org.evgem.android.bittorrentclient.ui.fragment.StartLoadingFragment

class LoadingsFragment : Fragment() {
    // views
    private lateinit var recycler: RecyclerView
    private lateinit var fab: FloatingActionButton

    // recycler stuff
    private val adapter = LoadingsAdapter()
    private lateinit var layoutManager: RecyclerView.LayoutManager

    private val serviceConnection = LoadingServiceConnection()

    // for runtime permission request
    private lateinit var pendingFileUri: Uri

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
            layoutManager = this@LoadingsFragment.layoutManager
            adapter = this@LoadingsFragment.adapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        //set listeners
        fab.setOnClickListener {
            sendTorrentIntent()
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(context, LoadingService::class.java)
        context?.startService(intent)
        context?.bindService(intent, serviceConnection, 0)
    }

    override fun onResume() {
        super.onResume()
        activity?.intent?.let { intent ->
            if (intent.action == Intent.ACTION_VIEW) {
                intent.data?.let { uri ->
                    processTorrentResult(uri)
                    activity?.intent = null
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        context?.unbindService(serviceConnection)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode) {
            TORRENT_FILE_REQUEST_CODE -> {
                data?.data?.let {
                    processTorrentResult(it)
                    return
                }
                Log.e(TAG, "onActivityResult: uri or intent is null")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST_CODE -> {
                if (permissions.isNotEmpty() && grantResults.isNotEmpty() &&
                    permissions[0] == Manifest.permission.READ_EXTERNAL_STORAGE &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    processTorrentResult(pendingFileUri)
                } else {
                    Toast.makeText(context, R.string.read_permission_error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun processTorrentResult(uri: Uri) {
        val activity = activity ?: return
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_EXTERNAL_STORAGE_REQUEST_CODE
            )
            pendingFileUri = uri
            return
        }

        val fileDescriptor = context?.contentResolver?.openFileDescriptor(uri, "r")?.fileDescriptor ?: return
        val dialog =
            StartLoadingFragment().apply { show(this@LoadingsFragment.fragmentManager, START_LOADING_FRAGMENT_TAG) }
        StartLoadingTask(dialog).execute(fileDescriptor)
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

    private inner class LoadingServiceConnection : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "disconnected from $name")
        }

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.i(TAG, "connected to $name")
            (binder as? LoadingService.LoadingBinder)?.let {
                it.loadings.observe(this@LoadingsFragment, Observer { newData ->
                    adapter.setLoadings(newData ?: return@Observer)
                    adapter.notifyDataSetChanged() // TODO notify only changed items
                })
            }
        }
    }

    companion object {
        private const val TORRENT_MIME_TYPE = "application/x-bittorrent"
        private const val TORRENT_FILE_REQUEST_CODE = 1231

        private const val READ_EXTERNAL_STORAGE_REQUEST_CODE = 761

        private const val TAG = "LoadingsFragment"

        private const val START_LOADING_FRAGMENT_TAG = "start loading fragment tag"
    }
}