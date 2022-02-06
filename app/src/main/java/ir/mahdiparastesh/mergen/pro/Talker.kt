package ir.mahdiparastesh.mergen.pro

import android.animation.ObjectAnimator
import android.net.Uri
import android.text.TextUtils
import android.util.Base64
import android.widget.Toast
import androidx.core.net.toUri
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import ir.mahdiparastesh.mergen.Panel
import ir.mahdiparastesh.mergen.Panel.Companion.handler
import ir.mahdiparastesh.mergen.R
import ir.mahdiparastesh.mergen.otr.AlertDialogue
import ir.mahdiparastesh.mergen.otr.UiTools
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern

class Talker(val c: Panel) {
    var said: String = c.b.say.text.toString()
    var result = true

    companion object {
        var isSending = false
        var anSending: ObjectAnimator? = null
    }

    init {
        result = true
        if (said == "" || isSending) result = false
        if (result) {
            sending(true)
            var got = "t=$said"
            Volley.newRequestQueue(c).add(
                StringRequest(
                    Request.Method.GET,
                    encode("https://pro.mahdiparastesh.ir?$got"),
                    { res ->
                        sending(false)
                        if (res.startsWith("Traceback "))
                            AlertDialogue.alertDialogue1(c, R.string.proError, res)
                        else {
                            val temp = File(c.cacheDir, "response.wav")
                            FileOutputStream(temp).apply {
                                write(Base64.decode(res, Base64.DEFAULT))
                                close()
                            }
                            handler?.obtainMessage(Panel.Action.TALK.ordinal, temp.toUri())
                                ?.sendToTarget()
                            temp.deleteOnExit()
                        }
                        c.m.res.value = said
                    },
                    {
                        sending(false)
                        Toast.makeText(c, "Connection failed: $it", Toast.LENGTH_SHORT).show()
                    }).setShouldCache(false).setTag("talk").setRetryPolicy(
                    DefaultRetryPolicy(
                        10000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    )
                )
            )
        }
    }

    private fun sending(bb: Boolean) {
        isSending = bb
        c.b.say.isEnabled = !bb
        anSending = UiTools.whirl(c.b.sending, if (bb) null else anSending)
        UiTools.drown(c.b.sendIcon, !bb)
    }

    private fun encode(uriString: String): String {
        if (TextUtils.isEmpty(uriString)) return uriString
        val allowedUrlCharacters = Pattern.compile(
            "([A-Za-z0-9_.~:/?\\#\\[\\]@!$&'()*+,;" + "=-]|%[0-9a-fA-F]{2})+"
        )
        val matcher = allowedUrlCharacters.matcher(uriString)
        var validUri: String? = null
        if (matcher.find()) validUri = matcher.group()
        if (TextUtils.isEmpty(validUri) || uriString.length == validUri!!.length)
            return uriString

        val uri = Uri.parse(uriString)
        val uriBuilder = Uri.Builder().scheme(uri.scheme).authority(uri.authority)
        for (path in uri.pathSegments) uriBuilder.appendPath(path)
        for (key in uri.queryParameterNames)
            uriBuilder.appendQueryParameter(key, uri.getQueryParameter(key))
        return uriBuilder.build().toString()
    }
}
