package org.evgem.android.bittorrentclient.data.bencode

sealed class BValue

//class BString(val value: String) : BValue()
class BString(val value: ByteArray) : BValue()

class BInteger(val value: Long) : BValue()
class BList(val value: MutableList<BValue>) : BValue()
class BMap(val value: LinkedHashMap<String, BValue>) : BValue()