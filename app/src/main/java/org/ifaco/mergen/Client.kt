package org.ifaco.mergen

import android.animation.ObjectAnimator
import android.net.Uri
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.ifaco.mergen.Panel.Companion.rec
import org.ifaco.mergen.audio.Speaker
import org.ifaco.mergen.more.AlertDialogue
import java.util.regex.Pattern

class Client(
    val that: Panel,
    state: Int,
    val et: EditText,
    val waitingView: View,
    var sendingIcon: View,
    var hearIcon: View,
    val model: Model,
    var said: String = et.text.toString()
) {
    var result = false

    companion object {
        var bSending = false
        var anSending: ObjectAnimator? = null
    }

    init {
        //if (here == null) result = false
        if (said == "" || bSending) result = false
        var toLearn = said.startsWith("learn ")
        if (toLearn && state != 3) {
            Speaker.pronounce(said)
            result = false
        }
        sending(true)
        var got = (if (!toLearn) "t=$said" else "l=${said.substring(6)}")
        if (Nav.here != null) got = got.plus("&y=${Nav.here!!.latitude}&x=${Nav.here!!.longitude}")
        Volley.newRequestQueue(Fun.c).add(
            StringRequest(Request.Method.GET, encode("http://82.102.10.134/?$got"), { res ->
                sending(false)
                var pronouncable = ""
                var toastable = ""
                var traceback = false
                val splitted = res.trim().split("\n")
                splitted.mapIndexed { i, it ->
                    if (traceback) return@mapIndexed
                    if (it.startsWith("Traceback ")) {
                        AlertDialogue.alertDialogue1(that, "Traceback", res)
                        traceback = true
                        Fun.shake()
                    } else if (!it.startsWith("{'vrb'"))
                        pronouncable = pronouncable.plus(it)
                            .plus(if (i < splitted.size - 1) "\n" else "")
                    else toastable = toastable.plus(it).plus("\n")
                }
                if (pronouncable != "") Speaker.pronounce(pronouncable)
                else Speaker.pronounce(said)
                model.mean.value = toastable
                model.res.value = pronouncable
                if (rec.continuous) rec.continueIt(waitingView)
            }, {
                sending(false)
                Toast.makeText(Fun.c, "ERROR: $it", Toast.LENGTH_SHORT).show()
                if (rec.continuous) rec.continueIt(waitingView)
            }).setTag("talk").setRetryPolicy(
                DefaultRetryPolicy(
                    rec.conDur.toInt(), 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )
            )
        )
        result = true
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