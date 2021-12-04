package ir.mahdiparastesh.mergen.man

import android.graphics.Bitmap
import android.os.CountDownTimer
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import ir.mahdiparastesh.mergen.Fun
import ir.mahdiparastesh.mergen.Fun.Companion.c
import ir.mahdiparastesh.mergen.Fun.Companion.vish
import ir.mahdiparastesh.mergen.Panel
import ir.mahdiparastesh.mergen.Panel.Action
import ir.mahdiparastesh.mergen.Panel.Companion.handler
import java.io.ByteArrayOutputStream

class Recorder(val p: Panel, val view: PreviewView) : ToRecord {
    private lateinit var camProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var camProvider: ProcessCameraProvider
    private lateinit var preview: Preview
    var canPreview = false
    var previewing = false
    var recording = false
    var aud: Audio? = null
    var time: Long = 0L
    var pool: StreamPool? = null

    companion object {
        const val FRAME = 1000L
        val SIZE = Size(Fun.dm.widthPixels, Fun.dm.heightPixels)
    }

    override fun on() {
        if (!canPreview || previewing) return
        pool = StreamPool(Connect(p.m.host, p.m.visPort))
        vish(view)
        previewing = true
        camProviderFuture = ProcessCameraProvider.getInstance(c)
        camProviderFuture.addListener({
            preview = Preview.Builder()
                .setTargetResolution(SIZE)
                .build().also { it.setSurfaceProvider(view.surfaceProvider) }
            camProvider = camProviderFuture.get().also {
                it.unbindAll()
                it.bindToLifecycle(p, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            }
        }, ContextCompat.getMainExecutor(c))
    }

    override fun begin() {
        time = 0L
        recording = true
        aud = Audio(p).apply { start() }
        handler?.obtainMessage(Action.TOGGLE.ordinal, true)?.sendToTarget()
        capture()
    }

    fun capture() {
        if (!recording) return
        ByteArrayOutputStream().apply {
            view.bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, this)
            pool?.add(toByteArray())
        }
        time++
        object : CountDownTimer(FRAME, FRAME) {
            override fun onTick(rem: Long) {}
            override fun onFinish() {
                capture()
            }
        }.start()
    }

    override fun end() {
        if (recording) handler?.obtainMessage(Action.TOGGLE.ordinal, false)?.sendToTarget()
        recording = false
        aud?.end()
    }

    override fun off() {
        if (!previewing || !canPreview) return
        vish(view, false)
        previewing = false
        camProvider.unbindAll()
        preview.setSurfaceProvider(null)
        view.removeAllViews()
        pool?.destroy()
        pool = null
    }

    override fun destroy() {
        if (!canPreview) return
        off()
        aud?.interrupt()
        aud = null
    }
}
