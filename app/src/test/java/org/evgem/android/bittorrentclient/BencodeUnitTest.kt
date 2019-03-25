package org.evgem.android.bittorrentclient

import org.evgem.android.bittorrentclient.data.bencode.*
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
import java.util.*
import kotlin.collections.ArrayList

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class BencodeUnitTest {
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

    @Test
    fun peerCommunicationTest() {
        val file = File("/home/evgem/Downloads/test.torrent")
        FileInputStream(file).use {
            val metainfo = BDecoder.decode(it) as BMap
            val info = metainfo.value["info"] as BMap
            val piecesSize = (info.value["pieces"] as BString).value.size
            val pieceSize = (info.value["piece length"] as BInteger).value
            val torrentSize = piecesSize / 20 * pieceSize

            val infoEncoded = ByteArrayOutputStream()
            BEncoder.encode(info, infoEncoded)

            val messageDigest = MessageDigest.getInstance("SHA-1")
            val hashInfo = messageDigest.digest(infoEncoded.toByteArray())

            val r = TrackerCommunicator("http://bt4.t-ru.org/ann").sendRequest(
                TrackerRequest(
                    hashInfo,
                    "1234567890poiuytrewq".toByteArray(),
                    15000,
                    0,
                    0,
                    torrentSize
                )
            )

            val cs = ArrayList<PeerCommunicator>()
            for (peer in r!!.peers!!) {
                val c: PeerCommunicator = PeerCommunicator().setOnSocketInitializedListener {
                    println("$peer socket initialized!")
                    handshake(hashInfo, "superpeeeriiiiiddddd".toByteArray())
                }.setOnHandshakeListener { reserved, infoHash, peerId ->
                    println("$peer Handshake! reserved: $reserved, info_hash: $infoHash, peer_id: $peerId")
                    bitfield(FixedBitSet(345))
                }.setOnBitfieldListener { bitSet ->
                    println("$peer Bitfield! $bitSet")
                    unchoke()
                    interested()
                }.setOnHaveListener { pieceIndex ->
                    println("$peer Have! pieceIndex: $pieceIndex")
                }.setOnChokeListener {
                    println("$peer choked!")
                }.setOnUnchokeListener {
                    println("$peer unchoked!")
                }.setOnInterestedListener {
                    println("$peer interested!")
                }.setOnUninterestedListener {
                    println("$peer not interested!")
                }
                c.start(peer)
                cs.add(c)
            }

            Thread.sleep(10000)
            for (c in cs) {
                if (c.running) {
                    c.stop()
                }
            }
        }

    }

    @Test
    @Ignore
    fun debug() {
        val file = File("/home/evgem/Downloads/test.torrent")
        FileInputStream(file).use {
            val metainfo = BDecoder.decode(it) as BMap
            val torrentInfo = getTorrentInfo(metainfo)
            println(torrentInfo)
        }
//        val sArr = ByteArray(10) {
//            if (it < 5) {
//                it.toByte()
//            } else {
//                0
//            }
//        }
//        println(sArr.toList())
//
//        val bs = FixedBitSet(BitSet().apply { set(80) })
//        bs.bits.set(87)
//        bs.bits.set(88)
//        println(bs)
//
//        val dArr = bs.toByteArray()
//        println(dArr.toList())

//        val ms = ByteArray(4) { it.toByte() }
//        val bb = ByteBuffer.wrap(ms)
//        println(bb.int)
//        ms[0] = 140.toByte()
//        println(ByteBuffer.wrap(ms).int)

//        Socket("retracker.local", 80).use {
//            val writer = it.getOutputStream().bufferedWriter()
//            writer.write("GET /announce HTTP/1.1\r\n")
//            writer.write("Host: retracker.local\r\n")
//            writer.write("Connection: close\r\n")
//            writer.write("Accept: text/plain\r\n")
//            writer.write("\r\n")
//            writer.flush()
//            val reader = it.getInputStream().bufferedReader()
//            while (reader.readLine() != "");
//            println(reader.readLine())
//            reader.close()
////            writer.write("Accept:")
//        }
//        val file = File("/home/evgem/Downloads/test.torrent")
//        FileInputStream(file).use {
//            val metainfo = BDecoder.decode(it) as BMap
//            val info = metainfo.value["info"] as BMap
//            val piecesSize = (info.value["pieces"] as BString).value.size
//            val pieceSize = (info.value["piece length"] as BInteger).value
//            val torrentSize = piecesSize / 20 * pieceSize
//
//            val infoEncoded = ByteArrayOutputStream()
//            BEncoder.encode(info, infoEncoded)
//
//            val messageDigest = MessageDigest.getInstance("SHA-1")
//            val hashInfo = messageDigest.digest(infoEncoded.toByteArray())
//
//            TrackerCommunicator("http://bt4.t-ru.org/ann").let {
//                val response = it.sendRequest(
//                    TrackerRequest(
//                        hashInfo,
//                        "1234567890poiuytrewq".toByteArray(),
//                        15000,
//                        0,
//                        0,
//                        torrentSize
//                    )
//                )
//                println(response)
//            }

//            val result = httpRequest(
//                "http://bt4.t-ru.org/ann",
//                "info_hash" to hashInfo,
//                "peer_id" to "1234567890poiuytrewq".toByteArray(),
//                "port" to "51413".toByteArray(),
//                "downloaded" to "0".toByteArray(),
//                "uploaded" to "0".toByteArray(),
//                "left" to 11574673001.toString().toByteArray(),
//                "event" to "stopped".toByteArray(),
//                "compact" to "1".toByteArray()
//            )
//            result?.let {
//                println(String(result))
//            }
//        }
//    }
//        }
    }
}