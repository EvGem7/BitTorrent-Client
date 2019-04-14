package org.evgem.android.bittorrentclient.async

import android.os.AsyncTask
import android.support.v4.app.DialogFragment
import org.evgem.android.bittorrentclient.data.bencode.BDecoder
import org.evgem.android.bittorrentclient.data.bencode.BMap
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import org.evgem.android.bittorrentclient.data.parse.getTorrentInfo
import java.io.FileDescriptor
import java.io.FileInputStream

/**
 * Parses .torrent file and passes received TorrentInfo to observer.
 */
class StartLoadingTask(private val observer: Observer) : AsyncTask<FileDescriptor, Void?, TorrentInfo?>() {
    interface Observer {
        /**
         * Main thread.
         */
        fun onTorrentInfoObtained(torrentInfo: TorrentInfo)
    }

    override fun doInBackground(vararg fileDescriptor: FileDescriptor?): TorrentInfo? {
        if (fileDescriptor.size != 1) {
            throw IllegalArgumentException("Only one file descriptor must be passed")
        }
        val input = FileInputStream(fileDescriptor[0])
        val root = BDecoder.decode(input) as? BMap ?: return null
        return getTorrentInfo(root)
    }

    override fun onPostExecute(result: TorrentInfo?) {
        observer.onTorrentInfoObtained(result ?: return)
    }
}