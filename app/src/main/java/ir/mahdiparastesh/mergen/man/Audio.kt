package ir.mahdiparastesh.mergen.man

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import ir.mahdiparastesh.mergen.Fun
import ir.mahdiparastesh.mergen.Model

class Audio(val m: Model) : Thread() {
    private var recorder: AudioRecord? = null
    private var buffer: ByteArray? = null
    private var minBufSize =
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)// 1000000
    private var time: Long = 0L
    private var pool = StreamPool(Connect(m.host, m.audPort))
    var active = true

    companion object {
        const val sampleRate = 44100
        const val channelConfig = AudioFormat.CHANNEL_IN_MONO
        const val audioFormat = AudioFormat.ENCODING_PCM_8BIT
    }

    @SuppressLint("MissingPermission")
    override fun run() {
        buffer = ByteArray(minBufSize)
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize
        )
        recorder!!.startRecording()
        val os = Fun.c.openFileOutput("audio.wav", Context.MODE_PRIVATE)
        while (active) {
            val bytesRead = recorder!!.read(buffer!!, 0, buffer!!.size, AudioRecord.READ_BLOCKING)
            //pool.add(StreamPool.Item(time, buffer!!))
            os.write(buffer, 0, bytesRead)
            time++
        }
        os.close()
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
