package org.evgem.android.bittorrentclient.async

import android.os.AsyncTask
import android.support.v4.app.DialogFragment
import android.util.Log
import org.evgem.android.bittorrentclient.data.bencode.BDecoder
import org.evgem.android.bittorrentclient.data.bencode.BMap
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import org.evgem.android.bittorrentclient.data.parse.getTorrentInfo
import org.evgem.android.bittorrentclient.exception.BEncodeException
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

        fun onError()
    }

    override fun doInBackground(vararg fileDescriptor: FileDescriptor?): TorrentInfo? {
        if (fileDescriptor.size != 1) {
            throw IllegalArgumentException("Only one file descriptor must be passed")
        }
        val input = FileInputStream(fileDescriptor[0])
        try {
            val root = BDecoder.decode(input) as? BMap ?: return null
            return getTorrentInfo(root)
        } catch (e: BEncodeException) {
            Log.e(TAG, Log.getStackTraceString(e))
        }
        return null
    }

    override fun onPostExecute(result: TorrentInfo?) {
        if (result == null) {
            observer.onError()
            return
        }
        observer.onTorrentInfoObtained(result)
    }

    companion object {
        private const val TAG = "StartLoadingTask"
    }
}