package ir.mahdiparastesh.mergen.otr

import android.content.DialogInterface
import android.graphics.Typeface
import android.text.util.Linkify
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import ir.mahdiparastesh.mergen.R

object AlertDialogue {
    fun alertDialogue1(
        c: BaseActivity, title: Int, message: Any,
        onOK: DialogInterface.OnClickListener? = null,
        onCancel: DialogInterface.OnCancelListener? = null
    ) {
        AlertDialog.Builder(c, R.style.alertDialogue1).apply {
            setTitle(title)
            if (message is String) setMessage(message)
            else if (message is Int) setMessage(message)
            setIcon(R.mipmap.launcher_round)
            setPositiveButton(android.R.string.ok, onOK)
            setOnCancelListener(onCancel)
        }.create().apply {
            show()
            fixADButton(c, getButton(AlertDialog.BUTTON_POSITIVE))
            fixADTitle(c, window)
            fixADMsg(c, window)
        }
    }

    fun fixADButton(
        c: BaseActivity, button: Button, sMargin: Boolean = false
    ) {
        button.apply {
            setTextColor(ContextCompat.getColor(c, R.color.CPO))
            setBackgroundColor(ContextCompat.getColor(c, R.color.CP))
            typeface = c.font()
            textSize = c.resources.getDimension(R.dimen.alert1Button) / c.dm.density
            if (sMargin) (layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = textSize.toInt()
            }
        }
    }

    fun fixADTitle(c: BaseActivity, window: Window?): TextView? {
        var tvTitle = window?.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
        tvTitle?.setTypeface(c.font(), Typeface.BOLD)
        tvTitle?.textSize = c.resources.getDimension(R.dimen.alert1Title) / c.dm.density
        tvTitle?.setTextColor(c.color(R.color.CP))
        return tvTitle
    }

    fun fixADMsg(
        c: BaseActivity, window: Window?, linkify: Boolean = false
    ): TextView? {
        var tvMsg = window?.findViewById<TextView>(android.R.id.message)
        tvMsg?.typeface = c.font()
        tvMsg?.setLineSpacing(
            c.resources.getDimension(R.dimen.alert1MsgLine) / c.dm.density, 0f
        )
        tvMsg?.textSize = c.resources.getDimension(R.dimen.alert1Msg) / c.dm.density
        tvMsg?.setTextColor(c.color(R.color.ADTC))
        tvMsg?.setPadding(c.dp(28), c.dp(15), c.dp(28), c.dp(15))
        if (tvMsg != null && linkify) Linkify.addLinks(tvMsg, Linkify.ALL)
        return tvMsg
    }
}
