package ir.mahdiparastesh.mergen

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.lifecycle.ViewModelProvider
import ir.mahdiparastesh.mergen.Fun.Companion.c
import ir.mahdiparastesh.mergen.Fun.Companion.permResult
import ir.mahdiparastesh.mergen.Fun.Companion.sp
import ir.mahdiparastesh.mergen.databinding.PanelBinding
import ir.mahdiparastesh.mergen.man.Connect
import ir.mahdiparastesh.mergen.man.Controller
import ir.mahdiparastesh.mergen.otr.DoubleClickListener
import ir.mahdiparastesh.mergen.pro.Writer

// adb connect 192.168.1.20:
// python C:\Users\fulcr\Projects\mergen\main.py

class Panel : AppCompatActivity() {
    private lateinit var b: PanelBinding
    private lateinit var m: Model
    private lateinit var pro: Writer
    private lateinit var man: Controller
    private var anRecording: ObjectAnimator? = null

    companion object {
        var handler: Handler? = null
        var mp: MediaPlayer? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = PanelBinding.inflate(layoutInflater)
        m = ViewModelProvider(this, Model.Factory()).get("Model", Model::class.java)
        setContentView(b.root)
        Fun.init(this, b.root)


        // Handlers
        handler = object : Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Action.WRITE.ordinal -> msg.obj?.let { b.say.setText("$it") }
                    Action.TALK.ordinal -> {
                        mp = MediaPlayer.create(this@Panel, msg.obj as Uri)
                        mp?.setOnPreparedListener { mp?.start() }
                        mp?.setOnCompletionListener { mp?.release(); mp = null }
                    }
                    Action.TOAST.ordinal -> try {
                        Toast.makeText(c, msg.obj as String, Toast.LENGTH_SHORT).show()
                    } catch (ignored: Exception) {
                        Toast.makeText(c, "INVALID MESSAGE", Toast.LENGTH_SHORT).show()
                    }
                    Action.SOCKET_ERROR.ordinal -> man.socketError(msg.obj as Connect.Error)
                    Action.TOGGLE.ordinal -> {
                        Fun.shift(
                            b.recording,
                            if (msg.obj as Boolean) R.drawable.indicator_1 else R.drawable.radar
                        )
                        Fun.vis(b.address, !(msg.obj as Boolean))
                    }
                    Action.FORCE_REC.ordinal -> man.rec.begin()
                    Action.WRONG.ordinal -> addrColour(true)
                    Action.PORTS.ordinal -> {
                        val ports = msg.obj as List<String>
                        for (s in man.manifest!!.sensors.indices) when (man.manifest!!.sensors[s].type) {
                            "aud" -> m.audPort.value = ports[s].toInt()
                            "toc" -> m.tocPort.value = ports[s].toInt()
                            "vis" -> m.visPort.value = ports[s].toInt()
                        }
                    }
                }
            }
        }

        // INITIALIZATION
        man = Controller(this, m, b.preview)
        pro = Writer(this, m, b.response, b.resSV, b.say, b.send, b.sendIcon, b.sending)

        // Connection
        val hint = "127.0.0.1".split(".")
        val addr = sp.getString(Controller.spHost, "")!!
        var ard: List<String>? = null
        if (addr != "") ard = addr.split(".")
        val adrETs = listOf(b.address1, b.address2, b.address3, b.address4)
        adrETs.forEachIndexed { i, et ->
            et.hint = hint[i]
            if (ard != null) et.setText(ard[i])
            et.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, r: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, r: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (errColoured) addrColour()
                    if (i != adrETs.size - 1 && s.toString().length == 3)
                        adrETs[i + 1].apply { if (text.isEmpty()) requestFocus() }
                }
            })
        }
        b.preview.setOnClickListener(object : DoubleClickListener() {
            override fun onDoubleClick() {
                val ar = arrayListOf<String>()
                adrETs.forEach { e -> ar.add(e.text.toString()) }
                m.host.value = ar.joinToString(".")
                man.toggle()
            }
        })
        b.recording.colorFilter = Fun.cf(R.color.CPO)
        anRecording = ObjectAnimator.ofFloat(b.recording, "rotation", 0f, 360f).apply {
            duration = 522
            interpolator = LinearInterpolator()
            repeatCount = Animation.INFINITE
            start()
        }
        b.recording.setOnClickListener { }
    }

    override fun onResume() {
        super.onResume()
        man.on()
    }

    override fun onPause() {
        super.onPause()
        man.end()
        man.off()
    }

    override fun onDestroy() {
        man.destroy()
        try {
            mp?.release()
        } catch (ignored: Exception) {
        }
        mp = null
        handler = null
        System.gc()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val b = permResult(grantResults)
        when (requestCode) {
            Controller.req -> if (b) man.permitted()
        }
    }

    var errColoured = false
    fun addrColour(red: Boolean = false) {
        errColoured = red
        for (x in 0 until b.address.childCount) if (b.address[x] is TextView)
            (b.address[x] as TextView).setTextColor(Fun.color(if (red) R.color.error else R.color.CS))
    }

    enum class Action { WRITE, TALK, TOAST, SOCKET_ERROR, TOGGLE, FORCE_REC, WRONG, PORTS }
}
