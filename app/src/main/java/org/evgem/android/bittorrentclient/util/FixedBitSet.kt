package org.evgem.android.bittorrentclient.util

import java.util.*

class FixedBitSet(private val sizeBytes: Int) {
    var bits: BitSet = BitSet()
        private set

    constructor(bitSet: BitSet) : this((bitSet.length() + 7) / 8) {
        bits = bitSet
    }

    constructor(byteArray: ByteArray) : this(byteArray.size) {
        bits = BitSet.valueOf(byteArray)
    }

    fun toByteArray(): ByteArray {
        return bits.toByteArray().copyOf(sizeBytes)
    }

    override fun toString(): String {
        return bits.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FixedBitSet

        if (sizeBytes != other.sizeBytes) return false
        if (bits != other.bits) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sizeBytes
        result = 31 * result + bits.hashCode()
        return result
    }
}