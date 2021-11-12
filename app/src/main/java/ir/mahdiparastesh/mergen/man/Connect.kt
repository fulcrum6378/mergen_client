package ir.mahdiparastesh.mergen.man

import androidx.lifecycle.MutableLiveData
import ir.mahdiparastesh.mergen.Fun
import ir.mahdiparastesh.mergen.Panel
import ir.mahdiparastesh.mergen.Panel.Companion.handler
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

class Connect(val host: MutableLiveData<String>, val port: Any) {
    var portValue = 0

    fun send(data: ByteArray?, foreword: Boolean = true, receive: Boolean = false): String? {
        handler?.obtainMessage(Panel.Action.TOAST.ordinal, "SENT: ${data?.size}")?.sendToTarget()
        portValue = if (port is Int) port else (port as MutableLiveData<Int>).value!!
        var ret: String? = null
        if (data == null) error("false")
        else try {
            var socket = Socket(host.value, portValue)
            if (portValue == Controller.port) Controller.succeeded(host.value!!)
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
        handler?.obtainMessage(Panel.Action.SOCKET_ERROR.ordinal, Error(sent, portValue))
            ?.sendToTarget()
    }

    data class Error(
        val e: String, val port: Int, val byController: Boolean = port == Controller.port
    )
}
