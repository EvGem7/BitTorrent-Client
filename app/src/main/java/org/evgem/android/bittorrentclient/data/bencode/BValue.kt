package org.evgem.android.bittorrentclient.data.bencode

sealed class BValue

data class BString(val value: ByteArray) : BValue() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BString

        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

data class BInteger(val value: Long) : BValue()
data class BList(val value: MutableList<BValue>) : BValue()
data class BMap(val value: LinkedHashMap<String, BValue>) : BValue()