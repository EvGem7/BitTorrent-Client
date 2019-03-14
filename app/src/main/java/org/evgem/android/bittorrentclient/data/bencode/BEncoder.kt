package org.evgem.android.bittorrentclient.data.bencode

import java.io.OutputStream

class BEncoder(private val out: OutputStream) {
    fun encode(value: BValue) = when (value) {
        is BString -> encodeString(value.value)//TODO haha. fix it
        is BInteger -> encodeInt(value.value)
        is BList -> encodeList(value.value)
        is BMap -> encodeMap(value.value)
    }

    private fun encodeString(string: ByteArray) {
        val size = string.size.toString().toByteArray()
        out.write(size)
        out.write(':'.toInt())
        out.write(string)
    }

    private fun encodeInt(int: Long) {
        out.write('i'.toInt())
        out.write(int.toString().toByteArray())
        out.write('e'.toInt())
    }

    private fun encodeList(list: List<BValue>) {
        out.write('l'.toInt())
        for (value in list) {
            encode(value)
        }
        out.write('e'.toInt())
    }

    private fun encodeMap(map: LinkedHashMap<String, BValue>) {
        out.write('d'.toInt())
        for ((key, value) in map) {
            encodeString(key.toByteArray())
            encode(value)
        }
        out.write('e'.toInt())
    }

    companion object {
        fun encode(value: BValue, out: OutputStream) = BEncoder(out).encode(value)
    }
}