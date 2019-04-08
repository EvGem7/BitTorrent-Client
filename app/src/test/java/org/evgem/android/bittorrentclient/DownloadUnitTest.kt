package org.evgem.android.bittorrentclient

import org.evgem.android.bittorrentclient.data.bencode.BDecoder
import org.evgem.android.bittorrentclient.data.bencode.BMap
import org.evgem.android.bittorrentclient.data.business.LoadingController
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import org.evgem.android.bittorrentclient.data.parse.getTorrentInfo
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class DownloadUnitTest {
    private val lock = Any()

    private inner class WaitThread : Thread(), LoadingController.Observer {
        private var downloaded = false

        override fun run() {
            super.run()
            synchronized(lock) {
                while (!downloaded) {
                    Thread.sleep(1000)
                }
            }
        }

        override fun onDownloaded() {
            downloaded = true
        }
    }

    @Test
    fun downloadTest() {
        val file = File("/home/evgem/Downloads/Сивухин Д.В. - Общий курс физики (в 5 томах)) [2002-2005, DjVu, RUS] [rutracker-949410].torrent")
        FileInputStream(file).use {
            val metainfo = BDecoder.decode(it) as BMap
            val torrentInfo: TorrentInfo = getTorrentInfo(metainfo) ?: return

            for (f in torrentInfo.files) {
                f.path = "/tmp/test/" + f.path
            }

            val waitThread = WaitThread().apply { start() }
            val loadingController = LoadingController(torrentInfo, waitThread)
            if (loadingController.start()) {
                println("successfully started!")
                synchronized(lock) {
                    println("downloaded!")
                    loadingController.stop()
                }
            } else {
                println("something went wrong :(")
            }
        }
    }
}