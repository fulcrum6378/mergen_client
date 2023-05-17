package ir.mahdiparastesh.mergen.man

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class StreamPool(val con: Connect) : ArrayList<ByteArray>() {
    val job: Job
    val period = 1000L
    var active = true

    init {
        job = CoroutineScope(Dispatchers.IO).launch { act() }
    }

    suspend fun act() {
        if (!active) return
        if (isNotEmpty()) {
            runBlocking { con.send(this@StreamPool[0]) }
            if (isNotEmpty()) removeAt(0)
        } else try {
            delay(period)
        } catch (ignored: InterruptedException) {
        }
        act()
    }

    fun destroy() {
        active = false
        clear()
        job.cancel()
    }
}
