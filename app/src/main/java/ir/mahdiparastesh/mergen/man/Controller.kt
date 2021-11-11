package ir.mahdiparastesh.mergen.man

import android.Manifest
import android.os.CountDownTimer
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import ir.mahdiparastesh.mergen.Fun.Companion.c
import ir.mahdiparastesh.mergen.Fun.Companion.permGranted
import ir.mahdiparastesh.mergen.Fun.Companion.sp
import ir.mahdiparastesh.mergen.Panel
import ir.mahdiparastesh.mergen.R
import ir.mahdiparastesh.mergen.otr.AlertDialogue
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

class Controller(val that: Panel, bPreview: PreviewView) : ToRecord {
    private var con = Connect()
    private var manifest: DevManifest? = null
    private var baManifest: ByteArray? = null
    val rec = Recorder(that, bPreview)
    var begun = false

    companion object {
        const val audPerm = Manifest.permission.RECORD_AUDIO
        const val visPerm = Manifest.permission.CAMERA
        const val req = 786
        const val spHost = "host"
        const val socketErrorTO = 1000L
        const val conTimeout = 1500L
        const val port = 3772
        const val spDeviceId = "device_id"
        val socketErrors = ArrayList<Connect.Error>()
        var socketErrorTimer: CountDownTimer? = null
        var host = "127.0.0.1"
        var audPort = 0
        var tocPort = 0
        var visPort = 0

        fun succeeded() {
            sp.edit().apply {
                putString(spHost, host)
                apply()
            }
        }
    }

    init {
        c.resources.openRawResource(R.raw.manifest).apply {
            baManifest = readBytes()
            close()
            manifest = Gson().fromJson(
                JsonReader(InputStreamReader(ByteArrayInputStream(baManifest))),
                DevManifest::class.java
            )
        }

        if (!permGranted(audPerm) || !permGranted(visPerm))
            ActivityCompat.requestPermissions(that, arrayOf(audPerm, visPerm), req)
        else permitted()
    }

    fun permitted() {
        rec.canPreview = true
        on()
    }

    var toggling = false
    fun toggle() {
        if (socketErrorTimer != null || toggling || !rec.previewing) return
        toggling = true
        if (!rec.recording) begin() else end()
    }

    fun acknowledge(times: Byte = 0) {
        if (!sp.contains(spDeviceId)) {
            val sigAckn =
                con.send(Notify.ACKN.s.plus(baManifest!!), foreword = false, receive = true)
            sp.edit().apply {
                putString(spDeviceId, sigAckn)
                apply()
            }
        }
        val sigInit = con.send(Notify.INIT.s.plus(queryId()), foreword = false, receive = true)
        if (sigInit == "false") acknowledge((times + 1).toByte())
        else if (sigInit?.startsWith("true") == true) {
            val ports = sigInit.substring(4).split(",")
            for (s in manifest!!.sensors.indices) when (manifest!!.sensors[s].type) {
                "aud" -> audPort = ports[s].toInt()
                "toc" -> tocPort = ports[s].toInt()
                "vis" -> visPort = ports[s].toInt()
            }
        }
    }

    fun queryId() = sp.getString(spDeviceId, "")!!.encodeToByteArray()

    override fun on() {
        rec.on()
    }

    override fun begin() {
        if (begun && rec.recording) return
        begun = true
        var ended = false
        val run = Thread {
            acknowledge()
            Panel.handler?.obtainMessage(Panel.Action.FORCE_REC.ordinal)?.sendToTarget()
            ended = true
        }
        object : CountDownTimer(conTimeout, conTimeout) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                if (ended) return
                run.interrupt()
                Panel.handler?.obtainMessage(
                    Panel.Action.SOCKET_ERROR.ordinal, Connect.Error("timedOut", port)
                )?.sendToTarget()
            }
        }.start()
        run.start()
        toggling = false
    }

    override fun end() {
        if (!begun && !rec.recording) return
        begun = false
        rec.end()
        Thread { con.send(Notify.HALT.s.plus(queryId()), foreword = false, receive = true) }.start()
        toggling = false
    }

    override fun off() {
        rec.off()
        Thread { con.send(Notify.KILL.s, foreword = false, receive = true) }.start()
    }

    override fun destroy() {
        rec.destroy()
    }

    fun socketError(e: Connect.Error) {
        if (!e.byController) end()
        socketErrors.add(e)
        socketErrorTimer?.cancel()
        socketErrorTimer = object : CountDownTimer(socketErrorTO, socketErrorTO) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                judgeSocketStatus()
                socketErrors.clear()
                socketErrorTimer = null
            }
        }.start()
    }

    fun judgeSocketStatus() {
        var wrong = false
        var conProblem = false
        var whichSock = "UNKNOWN"
        var whichAddr = whichSock
        var unknown: String? = null
        var sentNull = false
        for (e in socketErrors) {
            whichAddr = "$host:${e.port}"
            whichSock = when (e.port) {
                port -> "controller"
                audPort -> "audio"
                visPort -> "picture"
                else -> whichSock
            }
            when (e.e) {
                "java.net.NoRouteToHostException", "java.net.UnknownHostException",
                "timedOut", "wrongAnswer" -> wrong = true
                "java.net.ConnectException" -> conProblem = true
                "false" -> sentNull = true
                else -> unknown = e.e
            }
        }
        when {
            wrong -> AlertDialogue.alertDialogue1(
                that, R.string.recConnectErr, c.getString(R.string.recAddressErr)
            )
            unknown != null -> AlertDialogue.alertDialogue1(
                that, R.string.recConnectErr,
                c.getString(R.string.recSocketUnknownErr, unknown, whichSock)
            )
            sentNull -> AlertDialogue.alertDialogue1(
                that, R.string.recConnectErr,
                c.getString(R.string.recSocketSentNull, whichSock)
            )
            conProblem -> AlertDialogue.alertDialogue1(
                that, R.string.recConnectErr,
                c.getString(R.string.recSocketErr, whichSock, whichAddr)
            )
        }
        if (!sentNull) Panel.handler?.obtainMessage(Panel.Action.WRONG.ordinal)?.sendToTarget()
    }


    enum class Notify(val s: ByteArray) {
        ACKN("ackn".encodeToByteArray()),
        INIT("init".encodeToByteArray()),
        HALT("halt".encodeToByteArray()),
        KILL("kill".encodeToByteArray())
    }
}