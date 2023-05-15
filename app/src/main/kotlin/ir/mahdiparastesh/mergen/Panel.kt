package ir.mahdiparastesh.mergen

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ir.mahdiparastesh.mergen.databinding.PanelBinding
import ir.mahdiparastesh.mergen.mem.Connect
import ir.mahdiparastesh.mergen.mem.Controller

class Panel : ComponentActivity() {
    val c: Context get() = applicationContext
    val b: PanelBinding by lazy { PanelBinding.inflate(layoutInflater) }
    val m: Model by viewModels()
    val man: Controller by lazy { Controller(this) }
    val perm = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions -> if (permissions.entries.all { it.value }) man.permitted() }
    val sp: SharedPreferences by lazy { getPreferences(Context.MODE_PRIVATE) }

    companion object {
        var handler: Handler? = null
    }

    class Model : ViewModel() {
        val host = MutableLiveData("127.0.0.1")
        val audPort = MutableLiveData(0)
        val tocPort = MutableLiveData(0)
        val visPort = MutableLiveData(0)
        val toggling = MutableLiveData(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(b.root)
        man.init()

        handler = object : Handler(Looper.getMainLooper()) {
            @Suppress("UNCHECKED_CAST")
            @SuppressLint("SetTextI18n")
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Action.TOAST.ordinal -> try {
                        Toast.makeText(c, msg.obj as String, Toast.LENGTH_SHORT).show()
                    } catch (ignored: Exception) {
                        Toast.makeText(c, "INVALID MESSAGE", Toast.LENGTH_SHORT).show()
                    }
                    Action.SOCKET_ERROR.ordinal -> man.socketError(msg.obj as Connect.Error)
                    Action.TOGGLE.ordinal -> b.address.isVisible = !(msg.obj as Boolean)
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
        handler = null
        super.onDestroy()
    }

    var errColoured = false
    fun addrColour(red: Boolean = false) {
        errColoured = red
        for (x in 0 until b.address.childCount) if (b.address[x] is TextView)
            (b.address[x] as TextView).setTextColor(
                if (red) Color.parseColor("#EE0000")
                else themeColor(android.R.attr.textColor)
            )
    }

    @ColorInt
    fun ContextThemeWrapper.themeColor(@AttrRes attr: Int = android.R.attr.colorAccent) =
        TypedValue().apply {
            theme.resolveAttribute(attr, this, true)
        }.data

    abstract class DoubleClickListener(val span: Long = 500) : View.OnClickListener {
        private var times: Long = 0
        abstract fun onDoubleClick()
        override fun onClick(v: View) {
            if ((SystemClock.elapsedRealtime() - times) < span) onDoubleClick()
            times = SystemClock.elapsedRealtime()
        }
    }

    enum class Action {
        TOAST, SOCKET_ERROR, TOGGLE, FORCE_REC, WRONG, PORTS, TOGGLING_ENDED
    }
}
