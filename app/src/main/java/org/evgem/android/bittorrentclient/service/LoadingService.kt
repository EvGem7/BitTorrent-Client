package org.evgem.android.bittorrentclient.service

import android.app.Service
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.util.SparseArray
import org.evgem.android.bittorrentclient.data.business.LoadingController
import org.evgem.android.bittorrentclient.data.database.AppDatabase
import org.evgem.android.bittorrentclient.data.database.LoadingInfoDao
import org.evgem.android.bittorrentclient.data.entity.Loading
import org.evgem.android.bittorrentclient.data.entity.LoadingInfo
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class LoadingService : Service(), LoadingController.Observer {
    private val loadings = MutableLiveData<List<Loading>>()
    private val loadingInfoList = ArrayList<LoadingInfo>()

    private val controllers = ArrayList<LoadingController>()

    private var loopThread = LoopThread()

    private val oldDownloaded = SparseArray<Long>()
    private val oldUploaded = SparseArray<Long>()

    private val dao: LoadingInfoDao = AppDatabase.INSTANCE.loadingDao()

    private val pool = Executors.newFixedThreadPool(1)

    override fun onCreate() {
        super.onCreate()
        pool.submit {
            val stored = dao.getAll()
            for (loadingInfo in stored) {
                startController(loadingInfo)
            }
        }
    }

    override fun onDownloaded() {
        emit()
        saveLoadingsState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val torrentInfo: TorrentInfo =
            intent?.extras?.getParcelable(TORRENT_INFO_EXTRA) ?: return START_STICKY
        val loadingInfo = LoadingInfo(torrentInfo)
        val startedProperly = startController(loadingInfo)
        if (startedProperly) {
            pool.submit {
                loadingInfo.id = dao.insert(loadingInfo)
            }
            startLoopThread()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        startLoopThread()
        return LoadingBinder()
    }

    override fun onRebind(intent: Intent?) {
        startLoopThread()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopLoopThread()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        loopThread.stopIt()
        saveLoadingsState(true)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        loopThread.stopIt()
        saveLoadingsState(true)
    }

    @Synchronized
    private fun startController(loadingInfo: LoadingInfo): Boolean {
        val exists = controllers.find { it.torrentInfo == loadingInfo.torrentInfo } != null
        if (exists) {
            return false
        }
        val controller = LoadingController(loadingInfo, this)
        if (!controller.start()) {
            Log.e(TAG, "cannot start loading for ${controller.torrentInfo}")
            if (controller.status != LoadingController.Status.ERROR) {
                Log.e(TAG, "control error. Controller status: ${controller.status}")
            }
            return false
        }
        controllers += controller
        loadingInfoList += loadingInfo
        return true
    }

    private fun saveLoadingsState(wait: Boolean = false) {
        val thread = thread {
            dao.insertAll(loadingInfoList.filter {
                it.id != 0L
            })
        }
        if (wait) {
            thread.join() // TODO stupid hack
        }
    }

    private fun emit() {
        val toEmit = ArrayList<Loading>(controllers.size)
        for ((index, controller) in controllers.withIndex()) {
            val downloaded = controller.downloaded
            val uploaded = controller.uploaded
            val left = controller.torrentInfo.totalSize - downloaded

            val name = controller.torrentInfo.name
            val status = controller.status.name
            val progress = downloaded.toFloat() / controller.torrentInfo.totalSize * 100
            val downSpeed = (downloaded - oldDownloaded[index, 0]) / EMITTING_PERIOD
            val upSpeed = (uploaded - oldUploaded[index, 0]) / EMITTING_PERIOD
            val size = controller.torrentInfo.totalSize
            val eta = left.toDouble() / downSpeed
            val peersCount = 0
            val seedsCount = 0
            toEmit += Loading(
                name,
                status,
                progress,
                downSpeed.toString(),
                upSpeed.toString(),
                size.toString(),
                eta.toString(),
                peersCount,
                seedsCount
            )
            oldDownloaded.put(index, downloaded)
            oldUploaded.put(index, uploaded)
        }
        loadings.postValue(toEmit)
    }

    private fun startLoopThread() = synchronized(loopThread) {
        if (!loopThread.running && loopThread.state == Thread.State.NEW) {
            loopThread.start()
        }
    }

    private fun stopLoopThread() = synchronized(loopThread) {
        if (loopThread.running) {
            loopThread.stopIt()
            loopThread = LoopThread()
        }
    }

    inner class LoadingBinder : Binder() {
        val loadings: LiveData<List<Loading>> = this@LoadingService.loadings
    }

    private inner class LoopThread : Thread("emitting thread") {
        var running = false
            private set

        override fun run() {
            running = true
            while (running) {
                emit()
                Thread.sleep(EMITTING_PERIOD)
            }
        }

        fun stopIt() {
            running = false
        }
    }

    companion object {
        private const val TAG = "LoadingService"

        private const val EMITTING_PERIOD = 1000L

        const val TORRENT_INFO_EXTRA = "org.evgem.android.bittorrentclient.service.TORRENT_INFO_EXTRA"
    }
}