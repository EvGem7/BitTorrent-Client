package org.evgem.android.bittorrentclient.ui.fragment

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import org.evgem.android.bittorrentclient.R
import org.evgem.android.bittorrentclient.async.StartLoadingTask
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import org.evgem.android.bittorrentclient.service.LoadingService

class StartLoadingFragment : DialogFragment(), StartLoadingTask.Observer {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_start_loading, container, false)
    }

    override fun onTorrentInfoObtained(torrentInfo: TorrentInfo) {
        val intent = Intent(context, LoadingService::class.java).apply {
            putExtra(LoadingService.TORRENT_INFO_EXTRA, torrentInfo)
        }
        context?.startService(intent)
        dismiss()
    }
}