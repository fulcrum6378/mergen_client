package ir.mahdiparastesh.mergen.man

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.util.Rational
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.Bitmap
import ir.mahdiparastesh.mergen.Model
import java.io.ByteArrayOutputStream

class Recorder(val that: Panel, val m: Model, val bPreview: PreviewView) : ToRecord {
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var useCaseGroup: UseCaseGroup
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraSelector: CameraSelector
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    var canPreview = false
    var previewing = false
    var recording = false
    var aud: Audio? = null
    var time: Long = 0L
    var pool: StreamPool? = null

    companion object {
        const val FRAME = 1000L
        val size = Size(800, 400)
    }

    @SuppressLint("RestrictedApi")
    override fun on() {
        if (!canPreview || previewing) return
        pool = StreamPool(Connect(m.host, m.visPort))
        vish(bPreview)
        previewing = true
        cameraProviderFuture = ProcessCameraProvider.getInstance(c)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                preview = Preview.Builder()
                    .setTargetResolution(Size(Fun.dm.widthPixels, Fun.dm.heightPixels))
                    .build().also { it.setSurfaceProvider(bPreview.surfaceProvider) }
                cameraProvider.unbindAll()
                cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                imageCapture = ImageCapture.Builder()
                    .setTargetRotation(that.resources.configuration.orientation)
                    .setMaxResolution(size)
                    .build()

                useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(imageCapture)
                    .setViewPort(
                        ViewPort.Builder(
                            Rational(Fun.dm.heightPixels, Fun.dm.widthPixels), // HEIGHT * WIDTH
                            that.resources.configuration.orientation
                        ).build()
                    ).build()
                cameraProvider.bindToLifecycle(that, cameraSelector, useCaseGroup)
                // "CameraSelector.DEFAULT_BACK_CAMERA" instead of "cameraSelector"
            } catch (e: Exception) {
                Toast.makeText(c, "CAMERA INIT ERROR: ${e.message}", Toast.LENGTH_SHORT).show()
                canPreview = false
            }
        }, ContextCompat.getMainExecutor(c))
    }

    override fun off() {
        if (!previewing || !canPreview) return
        vish(bPreview, false)
        previewing = false
        preview.setSurfaceProvider(null)
        bPreview.removeAllViews()
        pool?.destroy()
        pool = null
    }

    override fun begin() {
        time = 0L
        recording = true
        aud = Audio(m).apply { start() }
        handler?.obtainMessage(Action.TOGGLE.ordinal, true)?.sendToTarget()
        capture()
    }

    fun capture() {
        if (!recording) return
        val stream = ByteArrayOutputStream()
        bPreview.bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        pool?.add(StreamPool.Item(time, stream.toByteArray()))
        time++
        object : CountDownTimer(FRAME, FRAME) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                capture()
            }
        }.start()
    }

    override fun end() {
        if (recording) handler?.obtainMessage(Action.TOGGLE.ordinal, false)?.sendToTarget()
        recording = false
        pool?.destroy()
        aud?.pool?.destroy()
        aud?.interrupt()
        aud = null
    }

    override fun destroy() {
        if (!canPreview) return
        cameraExecutor.shutdown()
        pool?.destroy()
        aud?.pool?.destroy()
    }
}
