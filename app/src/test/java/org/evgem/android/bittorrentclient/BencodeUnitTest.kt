package org.evgem.android.bittorrentclient

import org.evgem.android.bittorrentclient.data.bencode.*
import org.evgem.android.bittorrentclient.data.network.httpRequest
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import java.io.*

import java.net.Socket
import java.security.MessageDigest

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class BencodeUnitTest {
    @Test
    fun test() {
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
    @Ignore
    fun debug() {
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
        val file = File("/home/evgem/Downloads/test.torrent")
        FileInputStream(file).use {
            val metainfo = BDecoder.decode(it) as BMap
            val info = metainfo.value["info"] as BMap
            val piecesSize = (info.value["pieces"] as BString).value.size
//            val pieceSize = (info.value["piece length"] as BInteger).value
            val torrentSize = (piecesSize / 20).toString().toByteArray()

            val infoEncoded = ByteArrayOutputStream()
            BEncoder.encode(info, infoEncoded)

            val messageDigest = MessageDigest.getInstance("SHA-1")
            val hashInfo = messageDigest.digest(infoEncoded.toByteArray())

            val result = httpRequest(
                "http://bt4.t-ru.org/ann",
                "info_hash" to hashInfo,
                "peer_id" to "1234567890poiuytrewq".toByteArray(),
                "port" to "51413".toByteArray(),
                "numwant" to "0".toByteArray(),
                "downloaded" to "0".toByteArray(),
                "uploaded" to "0".toByteArray(),
                "left" to 11574673001.toString().toByteArray(),
                "event" to "stopped".toByteArray(),
                "compact" to "1".toByteArray(),
                "key" to "ohuenny_key".toByteArray()
            )
            result?.let {
                println(String(result))
            }
        }


    }
}
