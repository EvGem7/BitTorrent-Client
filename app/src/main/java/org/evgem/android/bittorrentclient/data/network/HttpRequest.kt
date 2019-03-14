package org.evgem.android.bittorrentclient.data.network

import android.util.Log
import java.net.Socket
import java.net.URL
import java.net.URLEncoder

private const val HTTP_PORT = 80
private const val TAG = "HttpRequest"
private const val SUCCESS_PREFIX = "HTTP/1.1 2" //2xx - success codes
private val CONTENT_LENGTH_REGEX = """Content-Length: (\d+)""".toRegex()

fun httpRequest(url: String, vararg params: Pair<String, ByteArray>): ByteArray? {
    val host: String
    val path: String
    URL(url).let {
        if (it.protocol != "http") {
            Log.e(TAG, "Protocol is not http but ${it.protocol}")
            return null
        }
        host = it.host
        path = it.path
    }
    val request = "GET $path?${parseParams(*params)} HTTP/1.1\r\n" +
            "Host: $host\r\n" +
            "User-Agent: BitTorrentClient" +
//            "Connection: close\r\n" +
            "Accept: */*\r\n" +
            "\r\n"
    Socket(host, HTTP_PORT).use { socket ->
        val writer = socket.getOutputStream().bufferedWriter()
        writer.write(request)
        writer.flush()

        val input = socket.getInputStream().buffered()

        val headerBuilder = StringBuilder()
        while (true) {
            var c = input.read()
            headerBuilder.append(c.toChar())
            if (c == '\r'.toInt()) {
                headerBuilder.append(input.read().toChar()) //append '\n'
                c = input.read()
                headerBuilder.append(c.toChar())
                if (c == '\r'.toInt()) {
                    headerBuilder.append(c.toChar()) //append '\n' again
                    break //we have "\r\n\r\n"
                }
            }
        }
        val header = headerBuilder.toString()

        if (!header.startsWith(SUCCESS_PREFIX)) {
            Log.e(TAG, "Error status code. Headers: $header")
            return null
        }

        val matchResult = CONTENT_LENGTH_REGEX.find(header)
        if (matchResult == null) {
            Log.e(TAG, "Error. Content-Length is not specified.")
            return null
        }
        val contentLength: Int = matchResult.groupValues[1].toInt()

        val result = ByteArray(contentLength)
        val read = input.read(result)
        if (read != contentLength) {
            Log.e(TAG, "Error. Cannot read entire server response.")
            return null
        }
        return result
    }
}

private fun parseParams(vararg params: Pair<String, ByteArray>): String {
    val builder = StringBuilder()
    for ((key, value) in params) {
        builder.append(key)
        builder.append('=')

        for (byte: Byte in value) {
            if (byte in '0'.toByte()..'9'.toByte() ||
                byte in 'a'.toByte()..'z'.toByte() ||
                byte in 'A'.toByte()..'Z'.toByte() ||
                byte == '.'.toByte() ||
                byte == '-'.toByte() ||
                byte == '_'.toByte() ||
                byte == '~'.toByte()
            ) {
                builder.append(byte.toChar())
            } else {
                builder.append(String.format("%%%02x", byte))
            }
        }

        builder.append('&')
    }
    if (builder.isNotEmpty()) {
        builder.setLength(builder.length - 1)
    }
    return builder.toString()
}