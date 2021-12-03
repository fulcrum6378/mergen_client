package ir.mahdiparastesh.mergen.man

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import ir.mahdiparastesh.mergen.Panel
import java.nio.ByteBuffer

class Audio(p: Panel) : Thread() {
    private var recorder: AudioRecord? = null
    private var buffer: ByteBuffer? = null
    private val minBufSize = AudioRecord.getMinBufferSize(sampleRate, chConfig, format) * 2
    private var time = 0
    private val frames = arrayListOf<ArrayList<Byte>>(arrayListOf())
    var pool = StreamPool(Connect(p.m.host, p.m.audPort))
    var active = true

    companion object {
        const val src = MediaRecorder.AudioSource.MIC
        const val sampleRate = 44100
        const val chConfig = AudioFormat.CHANNEL_IN_MONO
        const val format = AudioFormat.ENCODING_PCM_16BIT // ENCODING_PCM_FLOAT
    }

    @SuppressLint("MissingPermission")
    override fun run() {
        buffer = ByteBuffer.allocateDirect(minBufSize)
        recorder = AudioRecord(src, sampleRate, chConfig, format, minBufSize)
        recorder!!.startRecording()
        Thread {
            while (active) {
                sleep(Recorder.FRAME)
                frames.add(arrayListOf())
                time++
                pool.add(frames[time - 1].toByteArray())
            }
        }.start()
        while (active) {
            recorder!!.read(buffer!!, minBufSize)
            frames[time].addAll(buffer!!.array().toList())
        }
        active = false
    }

    fun end() {
        active = false
        recorder?.stop()
        recorder?.release()
        recorder = null
    }

    override fun interrupt() {
        pool.destroy()
        super.interrupt()
    }
}
