package ir.mahdiparastesh.mergen.mem

import kotlinx.coroutines.runBlocking

class StreamPool(val con: Connect) : ArrayList<ByteArray>() {
    val work = Thread { act() }
    val period = 1000L
    var active = true

    init {
        work.start()
    }

    fun act() {
        if (!active) return
        if (isNotEmpty()) {
            runBlocking { con.send(this@StreamPool[0]) }
            if (isNotEmpty()) removeAt(0)
        } else try {
            Thread.sleep(period)
        } catch (ignored: InterruptedException) {
        }
        act()
    }

    fun destroy() {
        active = false
        clear()
        work.interrupt()
    }
}
