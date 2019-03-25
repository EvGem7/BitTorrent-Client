package org.evgem.android.bittorrentclient.data.bencode

sealed class BValue {
    val string: ByteArray? get() = (this as? BString)?.value
    val integer: Long? get() = (this as? BInteger)?.value
    val list: MutableList<BValue>? get() = (this as? BList)?.value
    val map: LinkedHashMap<String, BValue>? get() = (this as? BMap)?.value
}

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