package com.example.klitaviy.loggingcacheimpl

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val logs: LinkedList<String> = LinkedList()
    private val compositeDisposable = CompositeDisposable()

    private val logsStoreSize = 10

    private val logsAddTimeStep = 500L
    private val logsStoreTimeStep = 500L
    private val logsStoreDelay = 250L

    private val maxLogsPerTime = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val logsWritter = Observable.interval(logsStoreTimeStep, TimeUnit.MILLISECONDS)
            .doOnEach { synchronized(logs) { log("Logs size before storing : ${logs.size}.") } }
            .flatMapSingle {
                synchronized(logs) {
                    val data = mutableListOf(*getLogsToStore().toTypedArray())
                    storeLogs(data)
                }
            }
            .flatMapCompletable { storedLogs ->
                removeLogs(storedLogs)
            }

        val random = Random()
        val loggsFiller = Observable.interval(logsAddTimeStep, TimeUnit.MILLISECONDS)
            .doOnNext {
                synchronized(logs) {
                    val count = random.nextInt(maxLogsPerTime)
                    for (i in 0 until count) {
                        logs.add("${System.currentTimeMillis()}:${random.nextInt(9000)}")
                    }
                    log("Added $count logs. Logs size after : ${logs.size}")
                }
            }

        loggerView.setOnClickListener {
            compositeDisposable.clear()
            logs.clear()
        }

        fab.setOnClickListener { view ->
            compositeDisposable.add(loggsFiller.subscribe({
                //                log("Logs Added.")
            }, {
                it.printStackTrace()
                log("Cannot add logs : $it")
                compositeDisposable.clear()
            }))
            compositeDisposable.add(logsWritter.subscribe({
                //                log("Logs Stored.")
            }, {
                it.printStackTrace()
                log("Cannot store logs : $it")
                compositeDisposable.clear()
            }))
        }
    }

    private fun doSync(action: Unit) {

    }

    private fun getLogsToStore(): List<String> = logs.subList(
        0,
        if (logs.size < logsStoreSize) logs.size else logsStoreSize
    )

    private fun removeLogs(logsToRemove: List<String>): Completable =
        Completable.fromAction {
            synchronized(logs) {
                for (i in 0 until logsToRemove.size) {
                    logs.remove()
                }
                log("Logs size after storing : ${logs.size}.")
            }
        }

    private fun storeLogs(logsToStore: List<String>): Single<List<String>> =
        Single.just(logsToStore).delay(logsStoreDelay, TimeUnit.MILLISECONDS)

    private fun log(message: String) {
        Log.d("my_awesome_tag", message)
//        runOnUiThread { loggerView.text = "$message\n${loggerView.text}" }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
