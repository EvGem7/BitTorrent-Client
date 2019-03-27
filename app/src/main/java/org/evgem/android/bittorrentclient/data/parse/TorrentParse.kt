package org.evgem.android.bittorrentclient.data.parse

import android.util.Log
import org.evgem.android.bittorrentclient.constants.HASH_SIZE
import org.evgem.android.bittorrentclient.data.bencode.BMap
import org.evgem.android.bittorrentclient.data.bencode.BValue
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo.File
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

fun getTorrentInfo(root: BMap): TorrentInfo? {
    val announces = HashSet<String>()
    root.value["announce"]?.string?.let {
        announces.add(String(it))
    } ?: return null
    root.value["announce-list"]?.list?.let { outerList ->
        for (list in outerList) {
            for (announce in list.list ?: return null) {
                announce.string?.let {
                    announces.add(String(it))
                }
            }
        }
    }

    val creationDate = root.value["creation date"]?.integer?.let { Date(it * 1000) }
    val comment = root.value["comment"]?.string?.let { String(it) }
    val createdBy = root.value["created by"]?.string?.let { String(it) }
    val encoding = root.value["encoding"]?.string?.let { String(it) }

    val info = root.value["info"]?.map ?: return null
    val pieceLength: Int = info["piece length"]?.integer?.toInt() ?: return null

    val allPieces: ByteArray = info["pieces"]?.string ?: return null
    if (allPieces.size % HASH_SIZE != 0) {
        Log.e(TAG, "pieces have wrong size: ${allPieces.size}")
        return null
    }
    val pieces = ArrayList<ByteArray>()
    for (offset in 0 until allPieces.size step HASH_SIZE) {
        pieces.add(allPieces.sliceArray(offset until offset + HASH_SIZE))
    }

    val files: List<TorrentInfo.File> = if (info["files"]?.list == null) {
        getSingleFile(info)
    } else {
        getMultipleFiles(info)
    } ?: return null

    return TorrentInfo(
        announces,
        pieceLength,
        pieces,
        files,
        creationDate,
        comment,
        createdBy,
        encoding
    )
}

private fun getSingleFile(info: LinkedHashMap<String, BValue>): List<TorrentInfo.File>? {
    val path = info["name"]?.string?.let { String(it) } ?: return null
    val length = info["length"]?.integer ?: return null
    return ArrayList<File>().apply { add(File(path, length)) }
}

private fun getMultipleFiles(info: LinkedHashMap<String, BValue>): List<TorrentInfo.File>? {
    val files = ArrayList<File>()
    val root = info["name"]?.string?.let { String(it) } ?: return null
    val filesList = info["files"]?.list ?: return null
    for (f in filesList) {
        f.map?.let { file ->
            val length = file["length"]?.integer ?: return null
            val pathList = file["path"]?.list ?: return null

            val pathBuilder = StringBuilder(root)
            pathBuilder.append('/')
            for (p in pathList) {
                p.string?.let {
                    pathBuilder.append(String(it))
                    pathBuilder.append('/')
                } ?: return null
            }
            if (pathBuilder.isEmpty()) {
                return null
            }
            pathBuilder.setLength(pathBuilder.length - 1) // remove last '/'
            val path = pathBuilder.toString()

            files.add(File(path, length))
        } ?: return null
    }
    return files
}

private const val TAG = "getTorrentInfo"