package org.evgem.android.bittorrentclient.service

import android.app.PendingIntent
import android.app.Service
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import org.evgem.android.bittorrentclient.R
import org.evgem.android.bittorrentclient.data.business.LoadingController
import org.evgem.android.bittorrentclient.data.business.LoadingController.Status.DOWNLOADING
import org.evgem.android.bittorrentclient.data.business.LoadingController.Status.SEEDING
import org.evgem.android.bittorrentclient.data.database.AppDatabase
import org.evgem.android.bittorrentclient.data.database.LoadingInfoDao
import org.evgem.android.bittorrentclient.data.entity.Loading
import org.evgem.android.bittorrentclient.data.entity.LoadingInfo
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import org.evgem.android.bittorrentclient.ui.activity.MainActivity
import org.evgem.android.bittorrentclient.util.SpeedCalculator
import org.evgem.android.bittorrentclient.util.getFormattedSpeed
import org.evgem.android.bittorrentclient.util.getFormattedTime
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class LoadingService : Service(), LoadingController.Observer {
    private val loadings = MutableLiveData<List<Loading>>()
    private val loadingInfoList = ArrayList<LoadingInfo>()

    private val controllers = ArrayList<LoadingController>()
    private val downloadCalculators = HashMap<LoadingController, SpeedCalculator>()
    private val uploadCalculators = HashMap<LoadingController, SpeedCalculator>()

    private var loopThread = LoopThread()

    private val dao: LoadingInfoDao = AppDatabase.INSTANCE.loadingDao()

    private val pool = Executors.newFixedThreadPool(1)

    private val notification
        get() = NotificationCompat.Builder(applicationContext, "loadings")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            .setContentText(applicationContext.getString(R.string.notification_text))
            .setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    Intent(applicationContext, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

    override fun onCreate() {
        super.onCreate()
        pool.submit {
            val stored = dao.getAll()
            for (loadingInfo in stored) {
                startController(loadingInfo)
            }
        }
    }

    override fun onDownloaded(controller: LoadingController) {
        downloadCalculators[controller]?.reset()
        emit()
        saveLoadingsState()
        checkForegroundState()
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
        checkForegroundState()
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
        for (controller in controllers) {
            val downloaded = controller.downloaded
            val uploaded = controller.uploaded
            val left = controller.torrentInfo.totalSize - downloaded

            val name = controller.torrentInfo.name
            val status = controller.status.value
            val progress = downloaded.toFloat() / controller.torrentInfo.totalSize * 100

            val downloadCalculator = downloadCalculators.getOrPut(controller) { SpeedCalculator() }
            downloadCalculator.put(downloaded)
            val uploadCalculator = uploadCalculators.getOrPut(controller) { SpeedCalculator() }
            uploadCalculator.put(uploaded)

            val downSpeed = getFormattedSpeed(downloadCalculator.speed)
            val upSpeed = getFormattedSpeed(uploadCalculator.speed)
            val size = controller.torrentInfo.totalSize
            val eta = if (downloadCalculator.speed != 0L) {
                getFormattedTime(left / downloadCalculator.speed)
            } else {
                "∞"
            }
            toEmit += Loading(
                name,
                status,
                progress,
                downSpeed,
                upSpeed,
                size.toString(),
                eta,
                ControllerWrapper(controller)
            )
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

    private fun checkForegroundState() {
        controllers.forEach {
            if (it.status == DOWNLOADING || it.status == SEEDING) {
                startForeground(NOTIFICATION_ID, notification)
                return
            }
        }
        stopForeground(true)
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

    inner class ControllerWrapper(private val controller: LoadingController) {
        fun start() {
            downloadCalculators[controller]?.reset()
            uploadCalculators[controller]?.reset()
            controller.start()
            checkForegroundState()
        }

        fun stop() {
            downloadCalculators[controller]?.reset()
            uploadCalculators[controller]?.reset()
            controller.stop()
            checkForegroundState()
            saveLoadingsState()
        }

        fun cancel() {
            controller.stop()
            controllers.remove(controller)
            downloadCalculators.remove(controller)
            uploadCalculators.remove(controller)
            pool.submit {
                loadingInfoList.find { it.torrentInfo == controller.torrentInfo }?.let {
                    loadingInfoList.remove(it)
                    dao.delete(it)
                }
            }
            checkForegroundState()
        }
    }

    companion object {
        private const val TAG = "LoadingService"

        private const val EMITTING_PERIOD = 1000L // in ms

        private const val NOTIFICATION_ID = 1

        private const val CHANNELL_ID = "loadings"

        const val TORRENT_INFO_EXTRA = "org.evgem.android.bittorrentclient.service.TORRENT_INFO_EXTRA"
    }
}