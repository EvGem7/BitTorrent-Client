package org.evgem.android.bittorrentclient.util

import java.util.*

class FixedBitSet(private val sizeBytes: Int) {
    var bits: BitSet = BitSet()
        private set

    constructor(bitSet: BitSet) : this((bitSet.length() + 7) / 8) {
        bits = bitSet
    }

    constructor(byteArray: ByteArray) : this(byteArray.size) {
        for ((byteIndex, byte) in byteArray.withIndex()) {
            for (bitIndex in 0 until Byte.SIZE_BITS) {
                val shifted = byte.toInt() shl bitIndex
                if ((shifted and HIGH_ONE) != 0) {
                    bits[byteIndex * Byte.SIZE_BITS + bitIndex] = true
                }
            }
        }
    }

    fun toByteArray(): ByteArray {
        val result = ByteArray(sizeBytes)
        for (byteIndex in 0 until sizeBytes) {
            var byte = 0
            for (bitIndex in 0 until Byte.SIZE_BITS) {
                if (bits[byteIndex * 8 + bitIndex]) {
                    val toShift = Byte.SIZE_BITS - 1 - bitIndex
                    byte = byte or (1 shl toShift)
                }
            }
            result[byteIndex] = byte.toByte()
        }
        return result
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

    companion object {
        private const val HIGH_ONE = 0x80
    }
}