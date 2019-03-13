package org.evgem.android.bittorrentclient

import android.app.Application
import org.evgem.android.bittorrentclient.data.entity.Loading
import java.lang.Thread.sleep
import java.util.*
import kotlin.collections.ArrayList

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread(Runnable {
            for (i in 1..20) {
                synchronized(observers) {
                    val random = Random()
                    val count = random.nextInt(100)
                    val list = ArrayList<Loading>()
                    for (j in 0 until count) {
                        val loading = Loading(
                            "torrent $j",
                            "Downloading",
                            random.nextFloat() * 100,
                            "6.66 Mb/s",
                            "13 kb/s",
                            "13 Gb",
                            "1h 3m",
                            random.nextInt(),
                            random.nextInt()
                        )
                        list.add(loading)
                    }
                    for (o in observers) {
                        o.observe(list)
                    }
                    println(list)
                }
                sleep(5000)
            }
        }).start()
    }

    interface TestObserver {
        fun observe(list: List<Loading>)
    }

    companion object {
        private val observers = ArrayList<TestObserver>()

        fun addObserver(observer: TestObserver) {
            synchronized(observers) {
                observers.add(observer)
            }
        }
    }
}