package org.ifaco.mergen.audio

import android.speech.tts.TextToSpeech
import org.ifaco.mergen.Fun

class Speaker {
    companion object : TextToSpeech.OnInitListener {
        var tts: TextToSpeech? = null

        override fun onInit(status: Int) {
        }

        fun init() {
            tts = TextToSpeech(Fun.c, this)
        }

        fun pronounce(str: String) {
            if (tts == null || str == "") return
            tts!!.voice = tts!!.voices!!.toMutableList()[1]
            tts!!.speak(str, TextToSpeech.QUEUE_ADD, null, "res")
        }

        fun destroy() {
            try {
                tts?.shutdown()
            } catch (ignored: Exception) {
            } finally {
                tts = null
            }
        }
    }
}