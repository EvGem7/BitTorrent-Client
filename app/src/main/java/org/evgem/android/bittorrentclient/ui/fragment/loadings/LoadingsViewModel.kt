package org.evgem.android.bittorrentclient.ui.fragment.loadings


import android.arch.lifecycle.ViewModel
import android.os.AsyncTask
import android.util.Log
import org.evgem.android.bittorrentclient.data.bencode.BDecoder
import java.io.InputStream

class LoadingsViewModel : ViewModel() {
    fun addNewLoading(torrentInputStream: InputStream) {
        TestAsyncTask(torrentInputStream).execute()
    }
}

class TestAsyncTask(private val inputStream: InputStream) : AsyncTask<Void?, Void?, Void?>() {
    override fun doInBackground(vararg params: Void?): Void? {
        Log.i("TEST", BDecoder.decode(inputStream).toString())
        return null
    }
}