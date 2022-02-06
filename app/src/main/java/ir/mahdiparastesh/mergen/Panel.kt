package ir.mahdiparastesh.mergen

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.get
import ir.mahdiparastesh.mergen.databinding.PanelBinding
import ir.mahdiparastesh.mergen.man.Connect
import ir.mahdiparastesh.mergen.man.Controller
import ir.mahdiparastesh.mergen.otr.BaseActivity
import ir.mahdiparastesh.mergen.otr.DoubleClickListener
import ir.mahdiparastesh.mergen.otr.UiTools
import ir.mahdiparastesh.mergen.otr.UiTools.Companion.permResult
import ir.mahdiparastesh.mergen.otr.UiTools.Companion.vis
import ir.mahdiparastesh.mergen.pro.Writer

// adb connect 192.168.1.20:
// python C:\Users\fulcr\Projects\mergen\main.py

class Panel : BaseActivity() {
    lateinit var b: PanelBinding
    lateinit var man: Controller
    private lateinit var pro: Writer
    private var anRecording: ObjectAnimator? = null
    private var proOn = false

    companion object {
        var handler: Handler? = null
        var mp: MediaPlayer? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = PanelBinding.inflate(layoutInflater)
        setContentView(b.root)
        man = Controller(this)
        man.init()
        pro = Writer(this)


        handler = object : Handler(Looper.getMainLooper()) {
            @Suppress("UNCHECKED_CAST")
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
                        UiTools.shift(
                            b.recording,
                            if (msg.obj as Boolean) R.drawable.indicator_1 else R.drawable.radar
                        )
                        b.address.vis(!(msg.obj as Boolean))
                    }
                    Action.FORCE_REC.ordinal -> man.rec.begin()
                    Action.WRONG.ordinal -> addrColour(true)
                    Action.PORTS.ordinal -> {
                        m.toggling.value = false
                        val ports = msg.obj as List<String>
                        for (s in man.manifest!!.sensors.indices) when (man.manifest!!.sensors[s].type) {
                            "aud" -> m.audPort.value = ports[s].toInt()
                            "toc" -> m.tocPort.value = ports[s].toInt()
                            "vis" -> m.visPort.value = ports[s].toInt()
                        }
                    }
                    Action.TOGGLING_ENDED.ordinal -> m.toggling.value = false
                }
            }
        }

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
        b.recording.colorFilter = cf(R.color.CPO)
        anRecording = ObjectAnimator.ofFloat(b.recording, "rotation", 0f, 360f).apply {
            duration = 522
            interpolator = LinearInterpolator()
            repeatCount = Animation.INFINITE
            start()
        }
        b.recording.setOnClickListener {
            if (man.begun) return@setOnClickListener
            PopupMenu(ContextThemeWrapper(c, R.style.Theme_MergenAndroid), it).apply {
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.ppPro -> {
                            proOn = !proOn
                            b.say.vis(proOn)
                            b.send.vis(proOn)
                            b.resSV.vis(proOn)
                            item.isChecked = proOn
                            true
                        }
                        else -> false
                    }
                }
                inflate(R.menu.panel)
                show()
                menu.findItem(R.id.ppPro).isChecked = proOn
            }
        }
        m.toggling.observe(this) { bool -> b.recording.vis(!bool) }
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

    override fun onRequestPermissionsResult(req: Int, perm: Array<String?>, grant: IntArray) {
        super.onRequestPermissionsResult(req, perm, grant)
        val b = permResult(grant)
        when (req) {
            Controller.req -> if (b) man.permitted()
        }
    }

    var errColoured = false
    fun addrColour(red: Boolean = false) {
        errColoured = red
        for (x in 0 until b.address.childCount) if (b.address[x] is TextView)
            (b.address[x] as TextView).setTextColor(color(if (red) R.color.error else R.color.CS))
    }

    enum class Action {
        WRITE, TALK, TOAST, SOCKET_ERROR, TOGGLE, FORCE_REC, WRONG, PORTS, TOGGLING_ENDED
    }
}
