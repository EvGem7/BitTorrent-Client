package org.evgem.android.bittorrentclient.util

import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction

fun FragmentManager.transaction(block: FragmentTransaction.() -> Unit) {
    beginTransaction().let {
        block(it)
        it.commit()
    }
}