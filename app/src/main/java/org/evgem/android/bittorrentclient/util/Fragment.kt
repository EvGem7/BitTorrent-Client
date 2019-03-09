package org.evgem.android.bittorrentclient.util

import android.support.v4.app.Fragment
import org.evgem.android.bittorrentclient.ui.activity.MainActivity

val Fragment.mainActivity: MainActivity? get() = activity?.let { it as MainActivity }