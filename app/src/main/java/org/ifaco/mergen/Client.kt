package org.ifaco.mergen

import android.Manifest
import android.content.DialogInterface
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Fun.Companion.dm
import org.ifaco.mergen.Fun.Companion.dp
import org.ifaco.mergen.Fun.Companion.permGranted
import org.ifaco.mergen.otr.AlertDialogue.Companion.alertDialogue2
import org.webrtc.*

class Client(val that: Panel) {
    init {
        permissions()
        // address()
    }

    companion object {
        const val LOCAL_TRACK_ID = "MERGEN"
    }

    fun address() {
        val et = EditText(c).apply {
            setPadding(dp(18), dp(12), dp(18), dp(12))
            textSize = that.resources.getDimension(R.dimen.alert1Title) / dm.density
            setText(R.string.cliDefIP)
        }
        alertDialogue2(
            that, R.string.cliConnect, R.string.cliConnectIP, et,
            DialogInterface.OnClickListener { _, _ ->
                Volley.newRequestQueue(c).add(
                    StringRequest(Request.Method.GET, "http://${et.text}/", { res ->
                        Toast.makeText(c, res, Toast.LENGTH_LONG).show()
                    }, {
                        Toast.makeText(c, "$it", Toast.LENGTH_LONG).show()
                    }).setShouldCache(false).setTag("client").setRetryPolicy(
                        DefaultRetryPolicy(
                            5000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                        )
                    )
                )
            })
    }

    fun permissions() {
        if (permGranted(Manifest.permission.RECORD_AUDIO) && permGranted(Manifest.permission.CAMERA))
            Panel.handler?.obtainMessage(Panel.Action.RECORD.ordinal)?.sendToTarget()
        else ActivityCompat.requestPermissions(
            that,
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
            Panel.reqRecord
        )
    }

    fun record(renderer: SurfaceViewRenderer) {
        // Initialising PeerConnectionFactory
        val options = PeerConnectionFactory.InitializationOptions.builder(c)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        // Configuring PeerConnectionFactory
        val rootEglBase: EglBase = EglBase.create()
        val peerConnectionFactory = PeerConnectionFactory
            .builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true)
            )
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            })
            .createPeerConnectionFactory()

        // Setting the video output
        renderer.setMirror(false)
        renderer.setEnableHardwareScaler(true)
        renderer.init(rootEglBase.eglBaseContext, null)

        // Getting the video source
        Camera2Enumerator(c).run {
            deviceNames.find { isBackFacing(it) }?.let {
                val videoCapturer = createCapturer(it, null)
                val localVideoSource = peerConnectionFactory.createVideoSource(false)
                val surfaceTextureHelper = SurfaceTextureHelper.create(
                    Thread.currentThread().name, rootEglBase.eglBaseContext
                )
                videoCapturer.initialize(surfaceTextureHelper, c, localVideoSource.capturerObserver)
                videoCapturer.startCapture(480, 640, 60) // width, height, frame per second
                peerConnectionFactory.createVideoTrack(LOCAL_TRACK_ID, localVideoSource)
                    .addSink(renderer)
            } ?: throw IllegalStateException()
        }
    }
}
