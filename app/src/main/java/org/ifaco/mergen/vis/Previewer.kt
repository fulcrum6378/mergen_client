package org.ifaco.mergen.vis

import android.Manifest
import android.annotation.SuppressLint
import android.util.Rational
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import org.ifaco.mergen.Fun
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Fun.Companion.dm
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SuppressLint("UnsafeExperimentalUsageError", "RestrictedApi")
class Previewer(val that: AppCompatActivity, val previewView: PreviewView) {
    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    lateinit var cameraProvider: ProcessCameraProvider
    lateinit var useCaseGroup: UseCaseGroup
    lateinit var preview: Preview
    lateinit var videoCapture: VideoCapture
    lateinit var cameraSelector: CameraSelector
    var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    var canPreview = false
    var previewing = false
    var recording = false

    companion object {
        const val reqCamPer = 868
        const val camPerm = Manifest.permission.CAMERA
        val size = Size(1280, 800)
    }

    fun granted() {
        canPreview = true
        this.start()
    }

    fun start() {
        if (!canPreview) {
            if (!Fun.permGranted(camPerm))
                ActivityCompat.requestPermissions(that, arrayOf(camPerm), reqCamPer)
            else granted()
            return
        }
        if (previewing) return
        previewing = true
        cameraProviderFuture = ProcessCameraProvider.getInstance(c)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                preview = Preview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                cameraProvider.unbindAll()
                cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                videoCapture = VideoCapture.Builder()
                    .setMaxResolution(size)
                    .build()
                useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(videoCapture)
                    .setViewPort(
                        ViewPort.Builder(
                            Rational(dm.widthPixels, dm.heightPixels),
                            that.resources.configuration.orientation
                        ).build()
                    )
                    .build()
                cameraProvider.bindToLifecycle(that, cameraSelector, useCaseGroup)
                // "CameraSelector.DEFAULT_BACK_CAMERA" instead of "cameraSelector"
            } catch (exc: Exception) {
                Toast.makeText(c, exc.javaClass.name, Toast.LENGTH_SHORT).show()
                canPreview = false
            }
        }, ContextCompat.getMainExecutor(c))
    }

    fun stop() {
        if (!previewing || !canPreview) return
        previewing = false
        preview.setSurfaceProvider(null)
        previewView.removeAllViews()
    }

    fun resume() {
        if (!previewing || recording) return
        val vid = File(c.cacheDir, "vision.mp4")
        videoCapture.startRecording(
            VideoCapture.OutputFileOptions.Builder(vid).build(),
            cameraExecutor, object : VideoCapture.OnVideoSavedCallback {
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                }
            })
    }

    fun pause() {
        if (!previewing || !recording) return
        videoCapture.stopRecording()
    }

    fun destroy() {
        if (!canPreview) return
        cameraExecutor.shutdown()
    }
}
