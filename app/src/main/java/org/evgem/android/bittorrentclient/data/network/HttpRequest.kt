package org.evgem.android.bittorrentclient.data.network

import android.util.Log
import org.evgem.android.bittorrentclient.constants.SOCKET_TIMEOUT
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL

private const val HTTP_PORT = 80
private const val TAG = "HttpRequest"
private const val SUCCESS_PREFIX = "HTTP/1.1 2" //2xx - success codes
private val CONTENT_LENGTH_REGEX = """Content-Length: (\d+)""".toRegex()

private const val USER_AGENT = "Android BitTorrent Client"
private const val ACCEPT_ENCODING = "gzip;q=1.0, deflate, identity"
private const val ACCEPT = "*/*"
private const val CONNECTION = "close"

fun httpRequest(url: String, vararg params: Pair<String, ByteArray>): ByteArray? = httpRequest(
    url,
    ArrayList<Pair<String, ByteArray>>().apply {
        for (param in params) {
            add(param)
        }
    }
)

fun httpRequest(url: String, params: List<Pair<String, ByteArray>>): ByteArray? {
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
    val request = "GET $path?${encodeParams(params)} HTTP/1.1\r\n" +
            "Host: $host\r\n" +
            "User-Agent: $USER_AGENT\r\n" +
            "Accept-Encoding: $ACCEPT_ENCODING\r\n" +
            "Accept: $ACCEPT\r\n" +
            "Connection: $CONNECTION\r\n" +
            "\r\n"
    Socket().use { socket ->
        try {
            socket.connect(InetSocketAddress(host, HTTP_PORT), SOCKET_TIMEOUT)
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Connection timeout expired")
            return null
        }

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
                    headerBuilder.append(input.read().toChar()) //append '\n' again
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

private fun encodeParams(params: List<Pair<String, ByteArray>>): String {
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