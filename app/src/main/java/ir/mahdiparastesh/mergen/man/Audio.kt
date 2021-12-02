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
    private var minBufSize = AudioRecord.getMinBufferSize(sampleRate, chConfig, format) * 2
    private var time: Long = 0L
    private var pool = StreamPool(Connect(p.m.host, p.m.audPort))
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
        while (active) {
            recorder!!.read(buffer!!, minBufSize)
            pool.add(StreamPool.Item(time, buffer!!.array()))
            time++
        }
    }

    override fun interrupt() {
        active = false
        recorder?.stop()
        recorder?.release()
        recorder = null
        pool.destroy()
        super.interrupt()
    }
}
