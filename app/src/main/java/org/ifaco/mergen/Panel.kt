package org.ifaco.mergen

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Fun.Companion.cf
import org.ifaco.mergen.Fun.Companion.drown
import org.ifaco.mergen.Fun.Companion.fBold
import org.ifaco.mergen.Fun.Companion.fRegular
import org.ifaco.mergen.Fun.Companion.fade
import org.ifaco.mergen.Fun.Companion.shake
import org.ifaco.mergen.databinding.PanelBinding
import org.ifaco.mergen.more.AlertDialogue
import org.ifaco.mergen.more.DoubleClickListener
import java.util.*

class Panel : AppCompatActivity(), TextToSpeech.OnInitListener {
    lateinit var b: PanelBinding
    lateinit var locreq: LocationRequest
    lateinit var srIntent: Intent
    var tts: TextToSpeech? = null
    val model: Model by viewModels()
    var here: Location? = null
    var recognizer: SpeechRecognizer? = null
    val conDurNormal = 5000L
    val conDurLonger = 60000L
    val typeDur = 87L

    companion object {
        var handler: Handler? = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = PanelBinding.inflate(layoutInflater)
        setContentView(b.root)
        Fun.init(this, b.root)

        tts = TextToSpeech(c, this)
        model.res.observe(this, { newName -> b.response.text = newName })
        b.sSayHint = "....."// can be changed later but won't survive a configuration change
        srIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        //locationPermission()
        recordPermission()


        // Handlers
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    0 -> Toast.makeText(c, "${msg.obj}", Toast.LENGTH_SHORT).show()
                    1 -> b.response.text = "${msg.obj}"
                }
            }
        }

        // Listening
        model.res.observe(this, { s ->
            resTyper = s
            typer?.cancel()
            typer = null
            b.response.text = ""
            respond(0)
        })
        b.say.typeface = fRegular
        b.body.setOnClickListener(object : DoubleClickListener() {
            override fun onDoubleClick() {
                if (bSending || listening) return
                if (continuer != null) doNotContinue()
                if (!send(3)) {
                    drown(b.hearIcon, true)
                    if (continuous) continueIt()
                } else drown(b.hearIcon, false)
            }
        })

        // Hearing
        b.hearIcon.drawable.apply { colorFilter = cf() }
        b.hear.setOnClickListener {
            if (bSending || listening) return@setOnClickListener
            if (continuer != null) {
                doNotContinue(); return@setOnClickListener; }
            continuous = false
            recognizer?.startListening(srIntent)
        }
        b.hear.setOnLongClickListener {
            if (bSending || listening) return@setOnLongClickListener false
            if (continuer != null) {
                doNotContinue(); return@setOnLongClickListener false; }
            continuous = true
            recognizer?.startListening(srIntent)
            true
        }

        // Sending
        b.sendingIcon.drawable.apply { colorFilter = cf() }
        b.response.setOnClickListener {
            if (model.res.value == "") return@setOnClickListener
            Toast.makeText(c, model.mean.value, Toast.LENGTH_LONG).show()
        }
        b.clear.setOnClickListener { clear() }
        b.response.typeface = fBold
    }

    override fun onDestroy() {
        recognizer?.destroy()
        recognizer = null
        try {
            tts?.shutdown()
        } catch (ignored: Exception) {
        } finally {
            tts = null
        }
        handler = null
        super.onDestroy()
    }

    override fun onInit(status: Int) {
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        when (requestCode) {
            reqLocPer -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    locationSettings()
                else exit()
            }
            reqRecPer -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                hear()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        when (requestCode) {
            reqLocSet -> if (resultCode == RESULT_OK) locate()
        }
    }


    val reqLocPer = 508
    val locPerm = Manifest.permission.ACCESS_COARSE_LOCATION

    @Suppress("unused")
    fun locationPermission() {
        if (ActivityCompat.checkSelfPermission(this, locPerm) == PackageManager.PERMISSION_GRANTED
        ) locationSettings()
        else ActivityCompat.requestPermissions(this, arrayOf(locPerm), reqLocPer)
    }

    val reqLocSet = 666
    val locInterval = 60000L * 2L
    fun locationSettings() {
        locreq = LocationRequest.create()
        locreq.interval = locInterval
        locreq.fastestInterval = locInterval / 2L
        locreq.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        val lrBuilder = LocationSettingsRequest.Builder().addLocationRequest(locreq)
        val lrTask = LocationServices.getSettingsClient(this)
            .checkLocationSettings(lrBuilder.build())
        lrTask.addOnSuccessListener(this) { locate() }
        lrTask.addOnFailureListener(this) { e ->
            if (e is ResolvableApiException) try {
                e.startResolutionForResult(this, reqLocSet)
            } catch (ignored: SendIntentException) {
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun locate() {
        var flpc = LocationServices.getFusedLocationProviderClient(c)
        flpc.requestLocationUpdates(
            locreq, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    if (locationResult != null)
                        for (location in locationResult.locations)
                            if (location.time >= here?.time ?: 0) here = location
                }
            }, Looper.getMainLooper()
        )
        flpc.lastLocation.addOnSuccessListener { location -> if (location != null) here = location }
            .addOnFailureListener { locationSettings() }
    }

    val reqRecPer = 786
    val recPerm = Manifest.permission.RECORD_AUDIO
    fun recordPermission() {
        if (ActivityCompat.checkSelfPermission(this, recPerm) == PackageManager.PERMISSION_GRANTED
        ) hear()
        else ActivityCompat.requestPermissions(this, arrayOf(recPerm), reqRecPer)
    }

    var listening = false
    var continuous = false
    fun hear() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(c)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
                drown(b.hearIcon, false)
                drown(b.hearing, true)
            }

            override fun onResults(res: Bundle?) {
                listening = false
                drown(b.hearing, false)
                var state = 0
                res?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.forEach { s -> state = understand(s) }
                when (state) {
                    0, 3 -> if (!send(state)) {
                        drown(b.hearIcon, true)
                        if (continuous) continueIt()
                    }
                    1, 2 -> {
                        drown(b.hearIcon, true)
                        if (state == 2 && continuous) continueIt()
                    }
                }
            }

            override fun onBeginningOfSpeech() {
                b.hearing.playAnimation()
            }

            override fun onEndOfSpeech() {
                b.hearing.pauseAnimation()
            }

            override fun onError(error: Int) {
                listening = false
                drown(b.hearing, false)
                drown(b.hearIcon, true)
                if (continuous) continueIt()
            }
        })
    }

    fun understand(s: String): Int {
        when (s.toLowerCase(Locale.getDefault())) {
            "shake", "vibrate" -> {
                shake(786L); return 2; }
            "clear", "clean" -> clear()
            "send", "send it" -> return 3
            "exit", "close" -> exit()
            "wait" -> return 2
            "wait longer", "wait long" -> {
                conDur = conDurLonger
                return 2
            }
            "cancel" -> {
                continuous = false; return 1; }
            else -> b.say.setText(s)
        }
        return 0
    }

    fun send(state: Int, said: String = b.say.text.toString()): Boolean {
        //if (here == null) return false
        if (said == "" || bSending) return false
        var toLearn = said.startsWith("learn ")
        if (toLearn && state != 3) {
            pronounce(said)
            return false
        }
        sending(true)
        var got = (if (!toLearn) "t=$said" else "l=${said.substring(6)}")
        if (here != null) got = got.plus("&y=${here!!.latitude}&x=${here!!.longitude}")
        Volley.newRequestQueue(c).add(
            StringRequest(Request.Method.GET, Fun.encode("http://82.102.10.134/?$got"), { res ->
                sending(false)
                var pronouncable = ""
                var toastable = ""
                var traceback = false
                val splitted = res.trim().split("\n")
                splitted.mapIndexed { i, it ->
                    if (traceback) return@mapIndexed
                    if (it.startsWith("Traceback ")) {
                        AlertDialogue.alertDialogue1(this, "Traceback", res)
                        traceback = true
                        shake()
                    } else if (!it.startsWith("{'vrb'"))
                        pronouncable = pronouncable.plus(it)
                            .plus(if (i < splitted.size - 1) "\n" else "")
                    else toastable = toastable.plus(it).plus("\n")
                }
                if (pronouncable != "") pronounce(pronouncable)
                else pronounce(said)
                model.mean.value = toastable
                model.res.value = pronouncable
                if (continuous) continueIt()
            }, {
                sending(false)
                Toast.makeText(c, "ERROR: $it", Toast.LENGTH_SHORT).show()
                if (continuous) continueIt()
            }).setTag("talk").setRetryPolicy(
                DefaultRetryPolicy(conDur.toInt(), 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            )
        )
        return true
    }

    var bSending = false
    var anSending: ObjectAnimator? = null
    fun sending(bb: Boolean) {
        bSending = bb
        b.say.isEnabled = !bb
        anSending = Fun.whirl(b.sendingIcon, if (bb) null else anSending)
        if (!bb) drown(b.hearIcon, true)
    }

    var resTyper = ""
    var typer: CountDownTimer? = null
    fun respond(which: Int) {
        if (resTyper.length <= which) return
        b.response.text = b.response.text.toString().plus(resTyper[which])
        typer = object : CountDownTimer(typeDur, typeDur) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                respond(which + 1)
                typer = null
            }
        }.apply { start() }
    }

    fun pronounce(str: String) {
        if (tts == null || str == "") return
        tts!!.voice = tts!!.voices!!.toMutableList()[1]
        tts!!.speak(str, TextToSpeech.QUEUE_ADD, null, "res")
    }

    fun clear() {
        b.say.setText("")
        model.res.value = ""
    }

    var continuer: CountDownTimer? = null
    var conDur = conDurNormal
    fun continueIt() {
        continuer?.cancel()
        continuer = null
        fade(b.waiting)
        continuer = object : CountDownTimer(conDur, 10) {
            override fun onTick(millisUntilFinished: Long) {
                b.waiting.background =
                    Fun.pieChart((100f / conDur.toFloat() * millisUntilFinished.toFloat()).toInt())
            }

            override fun onFinish() {
                fade(b.waiting, false)
                if (!continuous) return
                recognizer?.startListening(srIntent)
                conDur = conDurNormal
                continuer = null
            }
        }.apply { start() }
    }

    fun doNotContinue() {
        continuer?.cancel()
        continuer = null
        conDur = conDurNormal
        fade(b.waiting, false)
    }

    fun exit() {
        moveTaskToBack(true)
        Process.killProcess(Process.myPid())
        kotlin.system.exitProcess(1)
    }
}
