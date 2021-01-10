package org.ifaco.mergen.camera

import android.Manifest
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import org.ifaco.mergen.Fun
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Panel
import org.ifaco.mergen.Panel.Companion.handler
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Previewer(val that: AppCompatActivity, val previewView: PreviewView) {
    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    lateinit var cameraProvider: ProcessCameraProvider
    lateinit var preview: Preview
    lateinit var imageCapture: ImageCapture
    lateinit var cameraSelector: CameraSelector
    var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    var previewing = false

    companion object {
        const val reqCamPer = 868
        const val camPerm = Manifest.permission.CAMERA
    }

    init {
        if (!Fun.permGranted(camPerm))
            ActivityCompat.requestPermissions(that, arrayOf(camPerm), reqCamPer)
        // In Panel:
        //Capture.reqCamPer -> if (permResult(grantResults)) cap.start()
    }

    fun start() {
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
                imageCapture = ImageCapture.Builder()
                    .setTargetRotation(that.window.decorView.display.rotation)
                    .build()
                cameraProvider.bindToLifecycle(that, cameraSelector, imageCapture, preview)
                // "CameraSelector.DEFAULT_BACK_CAMERA" instead of "cameraSelector"
            } catch (exc: Exception) {
                Toast.makeText(c, exc.javaClass.name, Toast.LENGTH_SHORT).show()
                handler?.obtainMessage(Panel.Action.CANT_SEE.ordinal)?.sendToTarget()
            }
        }, ContextCompat.getMainExecutor(c))
    }

    fun pause() {
        if (!previewing) return
        previewing = false
        preview.setSurfaceProvider(null)
        previewView.removeAllViews()
    }

    fun capture() {
        /*val outputFileOptions = ImageCapture.OutputFileOptions.Builder(File(...)).build()
        imageCapture.takePicture(outputFileOptions, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException) {
                }
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                }
            })*/
    }

    fun destroy() {
        cameraExecutor.shutdown()
    }
}
