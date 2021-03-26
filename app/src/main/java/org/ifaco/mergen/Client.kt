package org.ifaco.mergen

import android.animation.ObjectAnimator
import android.net.Uri
import android.text.TextUtils
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.net.toUri
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Panel.Companion.handler
import org.ifaco.mergen.Panel.Companion.rec
import org.ifaco.mergen.more.AlertDialogue
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern

class Client(
    val that: Panel,
    val et: EditText,
    val waitingView: View,
    var sendingIcon: View,
    var hearIcon: View,
    val model: Model,
    var said: String = et.text.toString()
) {
    var result = true

    companion object {
        var bSending = false
        var anSending: ObjectAnimator? = null
    }

    init {
        result = true
        //if (here == null) result = false
        if (said == "" || bSending) result = false
        if (result) {
            sending(true)
            var got = "t=$said"
            if (Nav.here != null) got = got
                .plus("&y=${Nav.here!!.latitude}&x=${Nav.here!!.longitude}")
            Volley.newRequestQueue(c).add(
                StringRequest(Request.Method.GET, encode("http://82.102.10.134/?$got"), { res ->
                    sending(false)
                    if (res.startsWith("Traceback "))
                        AlertDialogue.alertDialogue1(that, "Traceback", res)
                    else {
                        val temp = File(c.cacheDir, "response.wav")
                        FileOutputStream(temp).apply {
                            write(Base64.decode(res, Base64.DEFAULT))
                            close()
                        }
                        handler?.obtainMessage(Panel.Action.PRO.ordinal, temp.toUri())?.sendToTarget()
                        temp.deleteOnExit()
                    }
                    model.res.value = said
                    if (rec.continuous) rec.continueIt(waitingView)
                }, {
                    sending(false)
                    Toast.makeText(c, "Connection failed: $it", Toast.LENGTH_SHORT).show()
                    if (rec.continuous) rec.continueIt(waitingView)
                }).setShouldCache(false).setTag("talk").setRetryPolicy(
                    DefaultRetryPolicy(
                        rec.conDur.toInt(), 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    )
                )
            )
        }
    }

    fun sending(bb: Boolean) {
        bSending = bb
        et.isEnabled = !bb
        anSending = Fun.whirl(sendingIcon, if (bb) null else anSending)
        if (!bb) Fun.drown(hearIcon, true)
    }

    fun encode(uriString: String): String {
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