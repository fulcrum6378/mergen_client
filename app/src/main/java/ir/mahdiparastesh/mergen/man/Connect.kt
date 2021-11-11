package ir.mahdiparastesh.mergen.man

import ir.mahdiparastesh.mergen.Fun
import ir.mahdiparastesh.mergen.Panel
import ir.mahdiparastesh.mergen.Panel.Companion.handler
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

class Connect(val port: Int = Controller.port) {
    fun send(data: ByteArray?, foreword: Boolean = true, receive: Boolean = false): String? {
        var ret: String? = null
        if (data == null) error("false")
        else try {
            var socket = Socket(Controller.host, port)
            Controller.succeeded()
            var output = socket.getOutputStream()
            var input = BufferedReader(InputStreamReader(socket.getInputStream()))
            if (foreword) output.write(Fun.z(data.size.toString()).encodeToByteArray() + data)
            else output.write(data)
            output.flush()
            if (receive) ret = input.readLine()
            input.close()
            output.close()
            socket.close()
            System.gc()
        } catch (e: Exception) {
            error(e.javaClass.name)
        }
        return ret
    }

    fun error(sent: String) {
        handler?.obtainMessage(Panel.Action.SOCKET_ERROR.ordinal, Error(sent, port))
            ?.sendToTarget()
    }

    data class Error(
        val e: String, val port: Int, val byController: Boolean = port == Controller.port
    )
}
