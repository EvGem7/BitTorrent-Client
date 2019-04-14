package org.evgem.android.bittorrentclient.constants

import java.util.*

val PEER_ID = "AndroidTC".let {
    val result = ByteArray(HASH_SIZE)
    Random().nextBytes(result)
    it.toByteArray().copyInto(result)
    return@let result
}

const val HASH_SIZE = 20 //sha-1 hash size

const val SOCKET_TIMEOUT = 10_000 // 10 seconds

const val HASH_ALGORITHM = "SHA-1"
