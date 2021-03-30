package org.ifaco.mergen.pro

import android.os.CountDownTimer
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import org.ifaco.mergen.Fun
import org.ifaco.mergen.Model
import org.ifaco.mergen.Panel
import org.ifaco.mergen.otr.DoubleClickListener

class Writer(
    val that: Panel,
    val model: Model,
    bBody: ConstraintLayout,
    val bResponse: TextView,
    val bResSV: ConstraintLayout,
    val bSay: EditText,
    val bClear: ImageView
) {
    init {
        model.res.observe(that, { s ->
            resTyper = s
            typer?.cancel()
            typer = null
            bResponse.text = ""
            resHider?.cancel()
            resHider = null
            Fun.fade(bResSV)
            respond(0)
        })
        bSay.typeface = Fun.fRegular
        bSay.addTextChangedListener { Fun.vish(bClear, it.toString().isNotEmpty()) }
        bBody.setOnClickListener(object : DoubleClickListener() {
            override fun onDoubleClick() {
                if (!Communicator.bSending) Communicator(that, bSay, model)
            }
        })
        bClear.setOnClickListener {
            bSay.setText("")
            model.res.value = ""
        }
        bResponse.typeface = Fun.fBold
    }

    companion object {
        const val typeDur = 87L
        const val resHideAfter = 20000L
    }

    var resTyper = ""
    var typer: CountDownTimer? = null
    var resHider: CountDownTimer? = null
    fun respond(which: Int) {
        if (resTyper.length <= which) {
            resHider = object : CountDownTimer(resHideAfter, resHideAfter) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    Fun.fade(this@Writer.bResSV, false)
                    bResponse.text = ""
                }
            }.start()
            return
        }
        bResponse.text = bResponse.text.toString().plus(resTyper[which])
        typer = object : CountDownTimer(typeDur, typeDur) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                respond(which + 1)
                typer = null
            }
        }.apply { start() }
    }
}
