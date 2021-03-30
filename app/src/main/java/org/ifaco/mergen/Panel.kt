package org.ifaco.mergen

import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import org.ifaco.mergen.pro.Client.Companion.bSending
import org.ifaco.mergen.Fun.Companion.fBold
import org.ifaco.mergen.Fun.Companion.fRegular
import org.ifaco.mergen.Fun.Companion.fade
import org.ifaco.mergen.Fun.Companion.permResult
import org.ifaco.mergen.Fun.Companion.vis
import org.ifaco.mergen.Fun.Companion.vish
import org.ifaco.mergen.audio.Recorder
import org.ifaco.mergen.camera.Previewer
import org.ifaco.mergen.databinding.PanelBinding
import org.ifaco.mergen.more.DoubleClickListener
import org.ifaco.mergen.pro.Client
import java.lang.Exception
import java.util.*

class Panel : AppCompatActivity() {
    lateinit var b: PanelBinding
    val model: Model by viewModels()
    val typeDur = 87L
    val resHideAfter = 20000L

    companion object {
        lateinit var pre: Previewer
        lateinit var ear: Recorder
        var handler: Handler? = null
        var mp: MediaPlayer? = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = PanelBinding.inflate(layoutInflater)
        setContentView(b.root)
        Fun.init(this, b.root)


        // Handlers
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Action.HEAR.ordinal -> ear = Recorder()
                    Action.CANT_SEE.ordinal -> vis(b.preview, false)
                    Action.WRITE.ordinal -> msg.obj?.let { b.say.setText("$it") }
                    Action.PRO.ordinal -> {
                        mp = MediaPlayer.create(this@Panel, msg.obj as Uri)
                        mp?.setOnPreparedListener { mp?.start() }
                        mp?.setOnCompletionListener { mp?.release(); mp = null }
                    }
                }
            }
        }

        // Initializations
        pre = Previewer(this, b.preview)
        Recorder.recordPermission(this)

        // Listening
        model.res.observe(this, { s ->
            resTyper = s
            typer?.cancel()
            typer = null
            b.response.text = ""
            resHider?.cancel()
            resHider = null
            fade(b.resSV)
            respond(0)
        })
        b.say.typeface = fRegular
        b.say.addTextChangedListener { vish(b.clear, it.toString().isNotEmpty()) }
        b.body.setOnClickListener(object : DoubleClickListener() {
            override fun onDoubleClick() {
                if (!bSending) Client(this@Panel, b.say, model)
            }
        })

        // Sending
        b.clear.setOnClickListener { clear() }
        b.response.typeface = fBold
    }

    override fun onResume() {
        super.onResume()
        if (!pre.previewing) pre.start() // pre.capture()
    }

    override fun onPause() {
        super.onPause()
        if (pre.previewing) pre.pause()
    }

    override fun onDestroy() {
        pre.destroy()
        try {
            mp?.release()
        } catch (ignored: Exception) {
        }
        mp = null
        handler = null
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Recorder.reqRecPer -> if (permResult(grantResults))
                handler?.obtainMessage(Action.HEAR.ordinal)?.sendToTarget()
        }
    }


    var resTyper = ""
    var typer: CountDownTimer? = null
    var resHider: CountDownTimer? = null
    fun respond(which: Int) {
        if (resTyper.length <= which) {
            resHider = object : CountDownTimer(resHideAfter, resHideAfter) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    fade(b.resSV, false)
                    b.response.text = ""
                }
            }.start()
            return
        }
        b.response.text = b.response.text.toString().plus(resTyper[which])
        typer = object : CountDownTimer(typeDur, typeDur) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                respond(which + 1)
                typer = null
            }
        }.apply { start() }
    }

    fun clear() {
        b.say.setText("")
        model.res.value = ""
    }


    enum class Action { HEAR, CANT_SEE, WRITE, PRO }
}
