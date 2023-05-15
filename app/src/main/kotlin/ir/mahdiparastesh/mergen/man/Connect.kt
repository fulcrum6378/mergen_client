package ir.mahdiparastesh.mergen.man

import androidx.lifecycle.MutableLiveData
import ir.mahdiparastesh.mergen.Panel
import ir.mahdiparastesh.mergen.Panel.Companion.handler
import ir.mahdiparastesh.mergen.man.Controller.Companion.z
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

class Connect(val host: MutableLiveData<String>, val port: Any, val onSuccess: (String) -> Unit) {
    var portValue = 0

    fun send(
        data: ByteArray,
        foreword: Boolean = true,
        receive: Boolean = false,
        reportErrors: Boolean = true
    ): String? {
        portValue = when (port) {
            is Int -> port
            is MutableLiveData<*> -> port.value!! as Int
            else -> throw IllegalArgumentException("Invalid port argument!")
        }
        var ret: String? = null
        try {
            var socket = Socket(host.value, portValue)
            if (portValue == Controller.port) onSuccess(host.value!!)
            var output = socket.getOutputStream()
            var input = BufferedReader(InputStreamReader(socket.getInputStream()))
            if (foreword) output.write(z(data.size.toString()).encodeToByteArray() + data)
            else output.write(data)
            output.flush()
            if (receive) ret = input.readLine()
            input.close()
            output.close()
            socket.close()
            System.gc()
        } catch (e: Exception) {
            if (reportErrors) error(e.javaClass.name)
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
