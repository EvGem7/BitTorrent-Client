package org.evgem.android.bittorrentclient.data.entity

import java.util.*

data class TorrentInfo(
    val announces: Set<String>, //for tracker requests

    //for downloading
    val pieceLength: Int,
    val pieces: List<ByteArray>,
    val files: List<File>,

    //additional information
    val creationDate: Date?,
    val comment: String?,
    val createdBy: String?,
    val encoding: String?
) {
    data class File(
        val path: String,
        val length: Long
    )
}