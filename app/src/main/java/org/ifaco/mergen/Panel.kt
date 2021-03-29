package org.ifaco.mergen

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.location.*
import org.ifaco.mergen.Client.Companion.bSending
import org.ifaco.mergen.Fun.Companion.cf
import org.ifaco.mergen.Fun.Companion.drown
import org.ifaco.mergen.Fun.Companion.fBold
import org.ifaco.mergen.Fun.Companion.fRegular
import org.ifaco.mergen.Fun.Companion.fade
import org.ifaco.mergen.Fun.Companion.permResult
import org.ifaco.mergen.Fun.Companion.vis
import org.ifaco.mergen.Fun.Companion.vish
import org.ifaco.mergen.audio.Recognizer
import org.ifaco.mergen.audio.Recorder
import org.ifaco.mergen.camera.Previewer
import org.ifaco.mergen.databinding.PanelBinding
import org.ifaco.mergen.more.DoubleClickListener
import java.lang.Exception
import java.util.*

class Panel : AppCompatActivity() {
    lateinit var b: PanelBinding
    val model: Model by viewModels()
    val typeDur = 87L
    val resHideAfter = 20000L
    val seeDisabled = 0.73f

    companion object {
        lateinit var rec: Recognizer
        lateinit var pre: Previewer
        lateinit var mic: Recorder
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
                    Action.CANT_HEAR.ordinal -> {
                        b.hear.setOnClickListener(null)
                        b.hear.setOnLongClickListener(null)
                        b.hearIcon.alpha = 0.25f
                    }
                    Action.HEAR.ordinal -> {
                        rec.hear(b.hearIcon, b.hearing, b.waiting)
                        mic = Recorder()
                    }
                    Action.HEARD.ordinal -> Client(
                        this@Panel, b.say, b.waiting, b.sendingIcon, b.hearIcon, model
                    ).apply {
                        if (!result) {
                            drown(hearIcon, true)
                            if (rec.continuous) rec.continueIt(waitingView)
                        }
                    }
                    Action.CANT_SEE.ordinal -> {
                        b.see.setOnClickListener(null)
                        b.see.setOnLongClickListener(null)
                        b.seeIV.alpha = 0.25f
                        vis(b.preview, false)
                    }
                    Action.WRITE.ordinal -> msg.obj?.let { b.say.setText("$it") }
                    Action.CLEAN.ordinal -> clear()
                    Action.PRO.ordinal -> {
                        mp = MediaPlayer.create(this@Panel, msg.obj as Uri)
                        mp?.setOnPreparedListener { mp?.start() }
                        mp?.setOnCompletionListener { mp?.release(); mp = null }
                    }
                    Action.EXIT.ordinal -> {
                        moveTaskToBack(true)
                        Process.killProcess(Process.myPid())
                        kotlin.system.exitProcess(1)
                    }
                }
            }
        }

        // Initializations
        //Nav.locationPermission()
        rec = Recognizer(this)
        pre = Previewer(this, b.preview)
        Recorder.recordPermission(this)

        // Listening
        b.sSayHint = "....."// can be changed later but won't survive a configuration change
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
                if (bSending || rec.listening) return
                if (rec.continuer != null) rec.doNotContinue(b.waiting)
                Client(this@Panel, b.say, b.waiting, b.sendingIcon, b.hearIcon, model).apply {
                    if (!result) {
                        drown(b.hearIcon, true)
                        if (rec.continuous) rec.continueIt(b.waiting)
                    } else drown(b.hearIcon, false)
                }
            }
        })


        // Hearing
        b.hearIcon.drawable.apply { colorFilter = cf() }
        b.hear.setOnClickListener {
            if (bSending || rec.listening) return@setOnClickListener
            if (rec.continuer != null) {
                rec.doNotContinue(b.waiting); return@setOnClickListener; }
            rec.continuous = false
            rec.start()
        }
        b.hear.setOnLongClickListener {
            if (bSending || rec.listening) return@setOnLongClickListener false
            if (rec.continuer != null) {
                rec.doNotContinue(b.waiting); return@setOnLongClickListener false; }
            rec.continuous = true
            rec.start()
            true
        }

        // Seeing
        b.fEyeAlpha = if (pre.previewing) 1f else seeDisabled
        b.see.setOnClickListener {
            if (!pre.previewing) pre.start()
            else pre.pause()
            vish(b.preview, pre.previewing)
            b.fEyeAlpha = if (pre.previewing) 1f else seeDisabled
        }
        b.see.setOnLongClickListener {
            pre.capture()
            true
        }

        // Sending
        b.sendingIcon.drawable.apply { colorFilter = cf() }
        b.clear.setOnClickListener { clear() }
        b.response.typeface = fBold
    }

    override fun onDestroy() {
        pre.destroy()
        rec.destroy()
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
            Nav.reqLocPer -> {
                if (permResult(grantResults)) Nav.locationSettings(this)
                else handler?.obtainMessage(Action.EXIT.ordinal)?.sendToTarget()
            }
            Recorder.reqRecPer -> if (permResult(grantResults))
                handler?.obtainMessage(Action.HEAR.ordinal)?.sendToTarget()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        when (requestCode) {
            Nav.reqLocSet -> if (resultCode == RESULT_OK) Nav.locate(this)
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


    enum class Action { CANT_HEAR, HEAR, HEARD, CANT_SEE, WRITE, PRO, CLEAN, EXIT }
}
