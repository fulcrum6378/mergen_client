package org.ifaco.mergen

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Fun.Companion.permResult
import org.ifaco.mergen.databinding.PanelBinding
import org.ifaco.mergen.ear.Recorder
import org.ifaco.mergen.pro.Writer
import org.ifaco.mergen.vis.Previewer
import org.webrtc.*
import java.util.*

class Panel : AppCompatActivity() {
    lateinit var b: PanelBinding
    val model: Model by viewModels()

    companion object {
        //lateinit var vis: Previewer
        lateinit var ear: Recorder
        const val LOCAL_TRACK_ID = "MERGEN"

        @SuppressLint("StaticFieldLeak")
        lateinit var pro: Writer
        var handler: Handler? = null
        var mp: MediaPlayer? = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = PanelBinding.inflate(layoutInflater)
        setContentView(b.root)
        Fun.init(this, b.root)


        // Handlers
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Action.HEAR.ordinal -> ear = Recorder()
                    Action.WRITE.ordinal -> msg.obj?.let { b.say.setText("$it") }
                    Action.PRO.ordinal -> {
                        mp = MediaPlayer.create(this@Panel, msg.obj as Uri)
                        mp?.setOnPreparedListener { mp?.start() }
                        mp?.setOnCompletionListener { mp?.release(); mp = null }
                    }
                }
            }
        }

        // Pro
        pro = Writer(this, model, b.body, b.response, b.resSV, b.say, b.clear)
        Client(this)

        // Ear
        Recorder.recordPermission(this)

        // Vis
        //vis = Previewer(this, b.preview)
        /*b.preview.setOnLongClickListener {
            if (!vis.recording) vis.resume()
            else vis.pause()
            true
        }*/

        // RTC
        Client(this)
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
        b.renderer.setMirror(false)
        b.renderer.setEnableHardwareScaler(true)
        b.renderer.init(rootEglBase.eglBaseContext, null)
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
                    .addSink(b.renderer)
            } ?: throw IllegalStateException()
        }
    }

    override fun onResume() {
        super.onResume()
        //vis.start()
    }

    override fun onPause() {
        super.onPause()
        //vis.stop()
    }

    override fun onDestroy() {
        //vis.destroy()
        try {
            mp?.release()
        } catch (ignored: Exception) {
        }
        mp = null
        handler = null
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Recorder.reqRecPer -> if (permResult(grantResults))
                handler?.obtainMessage(Action.HEAR.ordinal)?.sendToTarget()
            //Previewer.reqCamPer -> if (permResult(grantResults)) vis.granted()
        }
    }


    enum class Action { HEAR, WRITE, PRO }
}
