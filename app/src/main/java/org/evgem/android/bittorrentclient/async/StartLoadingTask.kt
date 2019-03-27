package org.evgem.android.bittorrentclient.async

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import org.evgem.android.bittorrentclient.data.bencode.BDecoder
import org.evgem.android.bittorrentclient.data.bencode.BMap
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import org.evgem.android.bittorrentclient.data.parse.getTorrentInfo
import org.evgem.android.bittorrentclient.service.LoadingService
import java.io.FileDescriptor
import java.io.FileInputStream
import java.lang.IllegalArgumentException
import java.lang.ref.WeakReference

/**
 * Parses .torrent file and passes received TorrentInfo to service.
 */
class StartLoadingTask(context: Context) : AsyncTask<FileDescriptor, Void?, TorrentInfo?>() {
    private val contextRef = WeakReference(context)

    override fun doInBackground(vararg fileDescriptor: FileDescriptor?): TorrentInfo? {
        if (fileDescriptor.size != 1) {
            throw IllegalArgumentException("Only one file descriptor must be passed")
        }
        val input = FileInputStream(fileDescriptor[0])
        val root = BDecoder.decode(input) as? BMap ?: return null
        return getTorrentInfo(root)
    }

    override fun onPostExecute(result: TorrentInfo?) {
        contextRef.get()?.let { context ->
            val intent = Intent(context, LoadingService::class.java)
                .putExtra(LoadingService.TORRENT_INFO_EXTRA, result)
            context.startService(intent)
        }
    }
}