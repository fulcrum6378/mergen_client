package ir.mahdiparastesh.mergen.man

import kotlinx.coroutines.runBlocking

class StreamPool(val con: Connect) : ArrayList<StreamPool.Item>() {
    val work = Thread { act() }
    val period = 1000L
    var active = true

    init {
        work.start()
    }

    fun act() {
        if (!active) return
        if (size > 0) {
            runBlocking { con.send(this@StreamPool[0].data) }
            if (size > 0) removeAt(0)
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

    data class Item(val time: Long, val data: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Item
            if (time != other.time) return false
            if (!data.contentEquals(other.data)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = time.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
}
