package ir.mahdiparastesh.mergen.man

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import ir.mahdiparastesh.mergen.Panel
import ir.mahdiparastesh.mergen.R

class Controller(val p: Panel) : ToRecord {
    val onSuccess: (String) -> Unit = { succeeded(it) }
    private var con = Connect(p.m.host, port, onSuccess)
    private var baManifest: ByteArray? = null
    val rec = Recorder(p)
    var manifest: DevManifest? = null
    var begun = false

    companion object {
        const val FRAME = 1000L
        const val spHost = "host"
        const val socketErrorTO = 1000L
        const val conTimeout = 1500L
        const val port = 3772
        const val spDeviceId = "device_id"
        val socketErrors = ArrayList<Connect.Error>()
        var socketErrorTimer: CountDownTimer? = null

        fun z(s: String): String {
            var add = ""
            for (x in 0..(9 - s.length)) add += "0"
            return add + s
        }
    }

    fun init() {
        baManifest = p.resources.openRawResource(R.raw.manifest).use { it.readBytes() }
        manifest = Gson().fromJson(
            baManifest!!.toString(Charsets.UTF_8), DevManifest::class.java
        )

        if (!permGranted(p.c, Manifest.permission.RECORD_AUDIO) ||
            !permGranted(p.c, Manifest.permission.CAMERA)
        ) p.perm.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
        else permitted()
    }

    fun permitted() {
        rec.canPreview = true
        on()
    }

    fun toggle() {
        if (socketErrorTimer != null || p.m.toggling.value == true || !rec.previewing) return
        p.m.toggling.value = true
        if (!rec.recording) begin() else end()
    }

    fun initialize() {
        if (!p.sp.contains(spDeviceId)) acknowledge()
        val sigInit = con.send(Notify.INIT.s.plus(queryId()), foreword = false, receive = true)
        when {
            sigInit == "false" -> acknowledge()
            sigInit?.startsWith("true") == true -> Panel.handler?.obtainMessage(
                Panel.Action.PORTS.ordinal, sigInit.substring(4).split(",")
            )?.sendToTarget()

            else -> initialize()
        }
    }

    fun succeeded(host: String) {
        p.sp.edit().apply {
            putString(spHost, host)
            apply()
        }
    }

    fun acknowledge() {
        val sigAckn = con.send(Notify.ACKN.s.plus(baManifest!!), foreword = false, receive = true)
        if (sigAckn != null) p.sp.edit().apply {
            putString(spDeviceId, sigAckn)
            apply()
        } else acknowledge()
    }

    fun queryId() = p.sp.getString(spDeviceId, "")!!.encodeToByteArray()

    override fun on() {
        rec.on()
    }

    override fun begin() {
        if (begun && rec.recording) return
        begun = true
        var ended = false
        val run = Thread {
            initialize()
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
                p.m.toggling.value = false
            }
        }.start()
        run.start()
    }

    override fun end() {
        if (!begun && !rec.recording) return
        rec.end()
        p.m.toggling.value = true

        Thread {
            while (rec.pool?.isNotEmpty() == true || rec.aud?.pool?.isNotEmpty() == true)
                Thread.sleep(500L)
            begun = false
            Thread { con.send(Notify.HALT.s.plus(queryId()), foreword = false, receive = true) }
                .start()
            Panel.handler?.obtainMessage(Panel.Action.TOGGLING_ENDED.ordinal)?.sendToTarget()
        }.start()
    }

    override fun off() {
        rec.off()
        Thread { con.send(Notify.KILL.s, foreword = false, receive = true, reportErrors = false) }
            .start()
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
        for (e in socketErrors) {
            whichAddr = "${p.m.host.value}:${e.port}"
            whichSock = when (e.port) {
                port -> "controller"
                p.m.audPort.value -> "audio"
                p.m.tocPort.value -> "touch"
                p.m.visPort.value -> "picture"
                else -> whichSock
            }
            when (e.e) {
                "java.net.NoRouteToHostException", "java.net.UnknownHostException", "timedOut" ->
                    wrong = true

                "java.net.ConnectException" -> conProblem = true
                else -> unknown = e.e
            }
        }
        if (socketErrors.isNotEmpty()) try {
            AlertDialog.Builder(p)
                .setTitle(R.string.recConnectErr).setMessage(
                    when {
                        wrong -> p.getString(R.string.recAddressErr)
                        unknown != null -> p.getString(
                            R.string.recSocketUnknownErr,
                            unknown,
                            whichSock
                        )
                        conProblem -> p.getString(R.string.recSocketErr, whichSock, whichAddr)
                        else -> "" // LOGICALLY IMPOSSIBLE
                    }
                ).show()
        } catch (_: WindowManager.BadTokenException) {
        } // activity not running
        Panel.handler?.obtainMessage(Panel.Action.WRONG.ordinal)?.sendToTarget()
    }

    fun permGranted(c: Context, perm: String) =
        ActivityCompat.checkSelfPermission(c, perm) == PackageManager.PERMISSION_GRANTED


    enum class Notify(val s: ByteArray) {
        ACKN("ackn".encodeToByteArray()),
        INIT("init".encodeToByteArray()),
        HALT("halt".encodeToByteArray()),
        KILL("kill".encodeToByteArray())
    }
}
