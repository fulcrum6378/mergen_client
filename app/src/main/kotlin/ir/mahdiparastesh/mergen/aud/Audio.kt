package ir.mahdiparastesh.mergen.aud

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import ir.mahdiparastesh.mergen.Panel
import ir.mahdiparastesh.mergen.man.Connect
import ir.mahdiparastesh.mergen.man.StreamPool
import java.nio.ByteBuffer

class Audio(p: Panel) : Thread() {
    private var recorder: AudioRecord? = null
    private var buffer: ByteBuffer? = null
    private val minBufSize = AudioRecord.getMinBufferSize(sampleRate, chConfig, format) * 2
    private var time = 0
    private val frames = arrayListOf<ArrayList<Byte>>(arrayListOf())
    var pool = StreamPool(Connect(p.m.host, p.m.audPort, p.man.onSuccess))
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
            var repeat = false
            while (active) {
                if (!repeat) {
                    sleep(1000L) // FIXME FRAME
                    frames.add(arrayListOf())
                    time++
                }
                repeat = try {
                    pool.add(frames[time - 1].toByteArray())
                    false
                } catch (ignored: ConcurrentModificationException) {
                    true
                }
            }
        }.start()
        while (active) {
            recorder!!.read(buffer!!, minBufSize)
            frames[time].addAll(buffer!!.array().toList())
        }
        active = false
    }

    fun end() {
        if (active) return
        active = false
        recorder?.stop()
        recorder?.release()
        recorder = null
    }

    override fun interrupt() {
        end()
        pool.destroy()
        super.interrupt()
    }
}
