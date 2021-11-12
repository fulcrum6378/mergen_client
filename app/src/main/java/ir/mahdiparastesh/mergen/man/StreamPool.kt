package ir.mahdiparastesh.mergen.man

class StreamPool(val con: Connect) : ArrayList<StreamPool.Item>() {

    override fun add(element: Item): Boolean {
        Thread {
            // Everything works fine as far as here
            con.send(element.data)
        }.start()
        return super.add(element)
    }

    @Suppress("RedundantOverride")
    override fun clear() {
        super.clear()
    }

    data class Item(val time: Long, val data: ByteArray)
}
