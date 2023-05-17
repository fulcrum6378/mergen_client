package ir.mahdiparastesh.mergen.vis

import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import com.google.common.util.concurrent.ListenableFuture
import ir.mahdiparastesh.mergen.Panel
import ir.mahdiparastesh.mergen.Panel.Action
import ir.mahdiparastesh.mergen.Panel.Companion.handler
import ir.mahdiparastesh.mergen.aud.Audio
import ir.mahdiparastesh.mergen.man.Connect
import ir.mahdiparastesh.mergen.man.Controller.Companion.FRAME
import ir.mahdiparastesh.mergen.man.StreamPool
import ir.mahdiparastesh.mergen.man.ToRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class Recorder(val p: Panel) : ToRecord {
    private val camProviderFuture: ListenableFuture<ProcessCameraProvider>
            by lazy { ProcessCameraProvider.getInstance(p) }
    private val camProvider: ProcessCameraProvider by lazy { camProviderFuture.get() }
    private lateinit var preview: Preview
    var canPreview = false
    var previewing = false
    var recording = false
    var aud: Audio? = null
    var time: Long = 0L
    var pool: StreamPool? = null
    val size = Size(p.resources.displayMetrics.widthPixels, p.resources.displayMetrics.heightPixels)

    override fun on() {
        if (!canPreview || previewing) return
        pool = StreamPool(Connect(p.m.host, p.m.visPort, p.man.onSuccess))
        p.b.preview.isInvisible = false
        previewing = true
        camProviderFuture.addListener({
            @Suppress("DEPRECATION")
            preview = Preview.Builder()
                .setTargetResolution(size)
                .build().also { it.setSurfaceProvider(p.b.preview.surfaceProvider) }
            camProvider.unbindAll()
            camProvider.bindToLifecycle(p, CameraSelector.DEFAULT_BACK_CAMERA, preview)
        }, ContextCompat.getMainExecutor(p.c))
    }

    override fun begin() {
        time = 0L
        recording = true
        aud = Audio(p).apply { start() }
        handler?.obtainMessage(Action.TOGGLE.ordinal, true)?.sendToTarget()
        CoroutineScope(Dispatchers.Main).launch { capture() }
    }

    suspend fun capture() {
        if (!recording) return
        ByteArrayOutputStream().apply {
            p.b.preview.bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, this)
            pool?.add(toByteArray())
        }
        time++
        delay(FRAME)
        capture()
    }

    override fun end() {
        if (recording) handler?.obtainMessage(Action.TOGGLE.ordinal, false)?.sendToTarget()
        recording = false
        aud?.end()
    }

    override fun off() {
        if (!previewing || !canPreview) return
        p.b.preview.isInvisible = true
        previewing = false
        camProvider.unbindAll()
        preview.setSurfaceProvider(null)
        p.b.preview.removeAllViews()
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
