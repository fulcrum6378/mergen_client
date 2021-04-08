package org.ifaco.mergen.com

import android.Manifest
import android.annotation.SuppressLint
import android.util.Base64
import android.util.Rational
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import org.ifaco.mergen.Fun
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Fun.Companion.dm
import org.ifaco.mergen.Panel
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SuppressLint("UnsafeExperimentalUsageError", "RestrictedApi")
class Watcher(val that: Panel, val previewView: PreviewView) {
    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    lateinit var cameraProvider: ProcessCameraProvider
    lateinit var useCaseGroup: UseCaseGroup
    lateinit var preview: Preview
    lateinit var videoCapture: VideoCapture
    lateinit var imageCapture: ImageCapture
    lateinit var cameraSelector: CameraSelector
    lateinit var cli: Client
    var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    var canPreview = false
    var previewing = false
    var recording = false

    companion object {
        const val req = 868
        const val perm = Manifest.permission.CAMERA
        const val PORT = 3772
        val size = Size(1200, 800)
        val vision = File(c.cacheDir, "vision.jpg")
    }

    init {
        if (!Fun.permGranted(perm))
            ActivityCompat.requestPermissions(that, arrayOf(perm), req)
        else {
            canPreview = true
            this.start()
        }
    }

    fun start() {
        if (!canPreview) return
        if (previewing) return
        previewing = true
        cameraProviderFuture = ProcessCameraProvider.getInstance(c)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                preview = Preview.Builder()
                    .setTargetResolution(Size(dm.widthPixels, dm.heightPixels))
                    .build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                cameraProvider.unbindAll()
                cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                /*videoCapture = VideoCapture.Builder()
                    .setMaxResolution(size)
                    .build()*/
                imageCapture = ImageCapture.Builder()
                    .setTargetRotation(that.resources.configuration.orientation)
                    .setMaxResolution(size)
                    .build()
                useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    //.addUseCase(videoCapture)
                    .addUseCase(imageCapture)
                    .setViewPort(
                        ViewPort.Builder(
                            Rational(dm.heightPixels, dm.widthPixels), // HEIGHT * WIDTH
                            that.resources.configuration.orientation
                        ).build()
                    )
                    .build()
                cameraProvider.bindToLifecycle(that, cameraSelector, useCaseGroup)
                cli = Client(that, PORT, object : Client.Repeat {
                    override fun execute() {
                        if (cli.output != null) capture()
                    }
                })
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

    @Suppress("unused")
    fun resumeRecording() {
        if (!previewing || recording) return
        val vid = File(c.cacheDir, "vision.mp4")
        if (vid.exists()) vid.delete()
        videoCapture.startRecording(
            VideoCapture.OutputFileOptions.Builder(vid).build(),
            cameraExecutor, object : VideoCapture.OnVideoSavedCallback {
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                }
            })
    }

    @Suppress("unused")
    fun pauseRecording() {
        if (!previewing || !recording) return
        videoCapture.stopRecording()
    }

    fun capture() { // CLIENT THREAD
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(vision).build()
        imageCapture.takePicture(outputFileOptions, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException) {
                    Panel.handler?.obtainMessage(
                        Panel.Action.TOAST.ordinal, "ImageCaptureException: ${error.message}"
                    )?.sendToTarget()
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    FileInputStream(vision).use {
                        cli.output!!.write(Base64.encodeToString(it.readBytes(), Base64.DEFAULT))
                        cli.output!!.flush()
                        it.close()
                    }
                    try {
                        vision.delete()
                    } catch (e: Exception) {
                        Panel.handler?.obtainMessage(
                            Panel.Action.TOAST.ordinal, "Could not delete the cached image: ${e.message}"
                        )?.sendToTarget()
                    }
                }
            })
    }

    fun destroy() {
        if (!canPreview) return
        cameraExecutor.shutdown()
        try {
            cli.interrupt()
        } catch (ignored: Exception) {
        }
    }
}
