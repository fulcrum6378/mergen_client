package org.ifaco.mergen.audio

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import com.airbnb.lottie.LottieAnimationView
import org.ifaco.mergen.Fun
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Panel
import org.ifaco.mergen.Panel.Companion.handler
import java.util.*

class Recognizer(that: Panel) {
    private var srIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    }
    private var recognizer: SpeechRecognizer? = null
    private val conDurNormal = 5000L
    private val conDurLonger = 60000L
    var listening = false
    var continuous = false
    var continuer: CountDownTimer? = null
    var conDur = conDurNormal
    var available = false

    init {
        available = SpeechRecognizer.isRecognitionAvailable(c)
        if (!available) {
            handler?.obtainMessage(Panel.Action.CANT_HEAR.ordinal)?.sendToTarget()
        } else Recorder.recordPermission(that)
    }

    fun hear(hearIcon: View, hearingView: LottieAnimationView, waitingView: View) {
        if (!available) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(c)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
                Fun.drown(hearIcon, false)
                Fun.drown(hearingView, true)
            }

            override fun onResults(res: Bundle?) {
                listening = false
                Fun.drown(hearingView, false)
                var state = 0
                res?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.forEach { s -> state = understand(s) }
                when (state) {
                    0, 3 -> handler?.obtainMessage(Panel.Action.HEARD.ordinal)?.sendToTarget()
                    1, 2 -> {
                        Fun.drown(hearIcon, true)
                        if (state == 2 && continuous) continueIt(waitingView)
                    }
                }
            }

            override fun onBeginningOfSpeech() {
                hearingView.playAnimation()
            }

            override fun onEndOfSpeech() {
                hearingView.pauseAnimation()
            }

            override fun onError(error: Int) {
                listening = false
                Fun.drown(hearingView, false)
                Fun.drown(hearIcon, true)
                if (continuous) continueIt(waitingView)
            }
        })
    }

    fun start() {
        if (!available) return
        recognizer?.startListening(srIntent)
    }

    fun understand(s: String): Int {
        if (!available) return -1
        when (s.toLowerCase(Locale.getDefault())) {
            "shake", "vibrate" -> {
                Fun.shake(786L); return 2; }
            "clear", "clean" ->
                handler?.obtainMessage(Panel.Action.CLEAN.ordinal)?.sendToTarget()
            "send", "send it" -> return 3
            "exit", "close" ->
                handler?.obtainMessage(Panel.Action.EXIT.ordinal)?.sendToTarget()
            "wait" -> return 2
            "wait longer", "wait long" -> {
                conDur = conDurLonger
                return 2
            }
            "cancel" -> {
                continuous = false; return 1; }
            else -> handler?.obtainMessage(Panel.Action.WRITE.ordinal, s)?.sendToTarget()
        }
        return 0
    }

    fun continueIt(waitView: View) {
        if (!available) return
        continuer?.cancel()
        continuer = null
        Fun.fade(waitView)
        continuer = object : CountDownTimer(conDur, 10) {
            override fun onTick(millisUntilFinished: Long) {
                waitView.background =
                    Fun.pieChart((100f / conDur.toFloat() * millisUntilFinished.toFloat()).toInt())
            }

            override fun onFinish() {
                Fun.fade(waitView, false)
                if (!continuous) return
                recognizer?.startListening(srIntent)
                conDur = conDurNormal
                continuer = null
            }
        }.apply { start() }
    }

    fun doNotContinue(waitView: View) {
        if (!available) return
        continuer?.cancel()
        continuer = null
        conDur = conDurNormal
        Fun.fade(waitView, false)
    }

    fun destroy() {
        if (!available) return
        recognizer?.destroy()
        recognizer = null
    }
}
