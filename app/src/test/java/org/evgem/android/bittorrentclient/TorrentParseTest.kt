package org.evgem.android.bittorrentclient

import org.evgem.android.bittorrentclient.data.bencode.*
import org.evgem.android.bittorrentclient.data.business.LoadingController
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import org.evgem.android.bittorrentclient.data.entity.TrackerRequest
import org.evgem.android.bittorrentclient.data.network.PeerCommunicator
import org.evgem.android.bittorrentclient.data.network.TrackerCommunicator
import org.evgem.android.bittorrentclient.data.parse.getTorrentInfo
import org.evgem.android.bittorrentclient.util.FixedBitSet
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.collections.ArrayList

class TorrentParseTest {
    @Test
    fun torrentParseTest() {
        val origin = File("/home/evgem/Downloads/test.torrent")
        val new = File("/home/evgem/Downloads/new.torrent")

        FileInputStream(origin).use { input ->
            val decoded = BDecoder(input).decode()
            FileOutputStream(new).use { output ->
                BEncoder(output).encode(decoded)
            }
        }

        FileInputStream(origin).use { os ->
            FileInputStream(new).use { ns ->
                do {
                    val o = os.read()
                    val n = ns.read()
                    assertEquals(o, n)
                } while (o != -1)
            }
        }
    }
}