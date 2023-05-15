package ir.mahdiparastesh.mergen.pro

import android.os.CountDownTimer
import androidx.core.widget.addTextChangedListener
import ir.mahdiparastesh.mergen.Panel
import ir.mahdiparastesh.mergen.otr.UiTools
import ir.mahdiparastesh.mergen.otr.UiTools.vish

class Writer(val c: Panel) {
    init {
        c.m.res.observe(c) { s ->
            resTyper = s
            typer?.cancel()
            typer = null
            c.b.response.text = ""
            resHider?.cancel()
            resHider = null
            UiTools.fade(c.b.resSV)
            respond(0)
        }
        c.b.say.typeface = c.font()
        c.b.say.addTextChangedListener { c.b.send.vish(it.toString().isNotEmpty()) }
        c.b.send.setOnClickListener {
            if (c.b.say.text.toString() != "" && !Talker.isSending) Talker(c)
        }
        c.b.response.typeface = c.font(true)
    }

    companion object {
        const val typeDur = 50L
        const val resHideAfter = 30L * 1000L
    }

    var resTyper = ""
    var typer: CountDownTimer? = null
    var resHider: CountDownTimer? = null
    fun respond(which: Int) {
        if (resTyper.length <= which) {
            resHider = object : CountDownTimer(resHideAfter, resHideAfter) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    UiTools.fade(c.b.resSV, false)
                    c.b.response.text = ""
                }
            }.start()
            return
        }
        c.b.response.text = c.b.response.text.toString().plus(resTyper[which])
        typer = object : CountDownTimer(typeDur, typeDur) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                respond(which + 1)
                typer = null
            }
        }.apply { start() }
    }
}
