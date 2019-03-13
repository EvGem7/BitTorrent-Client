package org.evgem.android.bittorrentclient.data.bencode

import org.evgem.android.bittorrentclient.exception.BEncodeException
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class BDecoder(private val input: InputStream) {
    fun decode(): BValue {
        val indicator: Char = input.readChar()
        return decode(indicator)
    }

    private fun decode(indicator: Char): BValue = when (indicator) {
        in '0'..'9' -> decodeString(indicator)
        'l' -> decodeList()
        'd' -> decodeMap()
        'i' -> decodeInt()
        else -> throw BEncodeException("Unknown indicator: $indicator")
    }


    private fun decodeList(): BList {
        val result = ArrayList<BValue>()
        var c = input.readChar()
        while (c != 'e') {
            result.add(decode(c))
            c = input.readChar()
        }
        return BList(result)
    }

    private fun decodeMap(): BMap {
        val result = HashMap<String, BValue>()
        var c = input.readChar()
        while (c != 'e') {
            if (c !in '0'..'9') {
                throw BEncodeException("Error while decoding dictionary. Key must be string")
            }
//            val key = String(decodeString(c).value)
            val key = decodeString(c).value
            result[key] = decode()
            c = input.readChar()
        }
        return BMap(result)
    }

    private fun decodeInt(): BInteger {
        var c = input.readChar()
        val sign = c == '-'
        if (sign) {
            c = input.readChar()
        }
        if (c !in '0'..'9') {
            throw BEncodeException("Error while decoding int. No digits after 'i' found.")
        }

        var num = 0L
        while (c in '0'..'9') {
            num *= 10
            num += c.number
            c = input.readChar()
        }
        if (c != 'e') {
            throw BEncodeException("Error while decoding int. 'e' excepted. Found $c")
        }

        if (sign) {
            if (num == 0L) {
                throw BEncodeException("Error while decoding int. Negative zero.")
            }
            num *= -1
        }
        return BInteger(num)
    }

    private fun decodeString(firstDigit: Char): BString {
        var size: Int = firstDigit.number
        var c = input.readChar()
        while (c in '0'..'9') {
            size *= 10
            size += c.number
            c = input.readChar()
        }
        if (c != ':') {
            throw BEncodeException("Error while decoding string. Colon expected. Found $c")
        }

        val buffer = ByteArray(size)
        var read = 0
        while (read < size) {
            read += input.read(buffer, read, size - read).let { if (it != -1) it else throw EOFException() }
        }

        return BString(String(buffer))
    }

    private fun InputStream.readChar() = read().toChar()

    private val Char.number: Int get() = (this - '0')
}