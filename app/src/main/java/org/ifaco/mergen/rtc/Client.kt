package org.ifaco.mergen.rtc

import android.Manifest
import android.content.DialogInterface
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Fun.Companion.dm
import org.ifaco.mergen.Fun.Companion.dp
import org.ifaco.mergen.Fun.Companion.permGranted
import org.ifaco.mergen.Panel
import org.ifaco.mergen.R
import org.ifaco.mergen.otr.AlertDialogue.Companion.alertDialogue2
import org.webrtc.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class Client(val that: Panel, val renderer: SurfaceViewRenderer) {
    private var peerConnection: PeerConnection? = null
    private val rootEglBase: EglBase = EglBase.create()
    private lateinit var localVideoSource: VideoSource
    private lateinit var videoCapturer: VideoCapturer

    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
            .createIceServer()
    )
    private val observer = object : PeerConnectionObserver() {
        override fun onIceCandidate(p0: IceCandidate?) {
            super.onIceCandidate(p0)
            signallingClient.send(p0)
            addIceCandidate(p0)
        }

        override fun onAddStream(p0: MediaStream?) {
            super.onAddStream(p0)
            //p0?.videoTracks?.get(0)?.addSink(remote_view)
        }
    }

    private lateinit var signallingClient: SignallingClient

    private val sdpObserver = object : AppSdpObserver() {
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)
            signallingClient.send(p0)
        }
    }

    init {
        permissions()
    }

    companion object {
        const val LOCAL_TRACK_ID = "MERGEN" // "local_track"
        const val LOCAL_STREAM_ID = "local_track"
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
                signallingClient = SignallingClient(createSignallingClientListener(), et.text.toString())
                call(sdpObserver)
                /*Volley.newRequestQueue(c).add(
                    object : StringRequest(Method.POST, "http://${et.text}/connect", { res ->
                        Toast.makeText(c, res, Toast.LENGTH_LONG).show()
                    }, Response.ErrorListener {
                        Toast.makeText(c, "${it.message}", Toast.LENGTH_LONG).show()
                    }) {
                        @Throws(AuthFailureError::class)
                        override fun getParams() = HashMap<String, String>().apply {
                            this["sdp"] = "[525]"
                            this["type"] = "[737]"
                            this["video_transform"] = "153151"
                        }
                    }.apply {
                        setShouldCache(false)
                        tag = "client"
                        retryPolicy = DefaultRetryPolicy(
                            5000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                        )
                    }
                )*/
                //Receiver().start()
            })
    }

    class Receiver : Thread() {
        private var output: PrintWriter? = null
        private var input: BufferedReader? = null

        override fun run() {
            val socket: Socket
            try {
                socket = Socket("192.168.1.9", 3772)
                output = PrintWriter(socket.getOutputStream())
                input = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (true)
                    try {
                        input!!.readLine()?.let {
                            Panel.handler?.obtainMessage(Panel.Action.SOCKET.ordinal, it)
                        }
                        output!!.write("lll")
                        output!!.flush()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
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

    fun record() {
        // Initialising PeerConnectionFactory
        val options = PeerConnectionFactory.InitializationOptions.builder(c)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        // Configuring PeerConnectionFactory
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
        peerConnection = peerConnectionFactory.createPeerConnection(iceServer, observer)!!

        // Setting the video output
        renderer.setMirror(false)
        renderer.setEnableHardwareScaler(true)
        renderer.init(rootEglBase.eglBaseContext, null)
        // All 3 statements should be done for the remote view, including the next statements

        // Getting the video source
        Camera2Enumerator(c).run {
            deviceNames.find { isBackFacing(it) }?.let {
                videoCapturer = createCapturer(it, null)
                localVideoSource = peerConnectionFactory.createVideoSource(false)
                val surfaceTextureHelper = SurfaceTextureHelper.create(
                    Thread.currentThread().name, rootEglBase.eglBaseContext
                )
                videoCapturer.initialize(surfaceTextureHelper, c, localVideoSource.capturerObserver)
                videoCapturer.startCapture(480, 640, 60) // width, height, frame per second

                /*val localVideoTrack =
                    peerConnectionFactory.createVideoTrack(LOCAL_TRACK_ID, localVideoSource)
                localVideoTrack.addSink(localVideoOutput)
                val localStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)
                localStream.addTrack(localVideoTrack)
                peerConnection?.addStream(localStream)*/

                peerConnectionFactory.createVideoTrack(LOCAL_TRACK_ID, localVideoSource)
                    .addSink(renderer)
            } ?: throw IllegalStateException()
        }
    }

    private fun createSignallingClientListener() = object : SignallingClientListener {
        override fun onConnectionEstablished() {
            //call_button.isClickable = true
        }

        override fun onOfferReceived(description: SessionDescription) {
            onRemoteSessionReceived(description)
            answer(sdpObserver)
            //remote_view_loading.isGone = true
        }

        override fun onAnswerReceived(description: SessionDescription) {
            onRemoteSessionReceived(description)
            //remote_view_loading.isGone = true
        }

        override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
            addIceCandidate(iceCandidate)
        }
    }

    private fun PeerConnection.call(sdpObserver: SdpObserver) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        createOffer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {

                setLocalDescription(object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                    }

                    override fun onSetSuccess() {
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {
                    }

                    override fun onCreateFailure(p0: String?) {
                    }
                }, desc)
                sdpObserver.onCreateSuccess(desc)
            }
        }, constraints)
    }

    private fun PeerConnection.answer(sdpObserver: SdpObserver) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        createAnswer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                setLocalDescription(object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                    }

                    override fun onSetSuccess() {
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {
                    }

                    override fun onCreateFailure(p0: String?) {
                    }
                }, p0)
                sdpObserver.onCreateSuccess(p0)
            }
        }, constraints)
    }

    fun call(sdpObserver: SdpObserver) = peerConnection?.call(sdpObserver)

    fun answer(sdpObserver: SdpObserver) = peerConnection?.answer(sdpObserver)

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetFailure(p0: String?) {
            }

            override fun onSetSuccess() {
            }

            override fun onCreateSuccess(p0: SessionDescription?) {
            }

            override fun onCreateFailure(p0: String?) {
            }
        }, sessionDescription)
    }

    fun addIceCandidate(iceCandidate: IceCandidate?) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun callMe() {
        address()
    }

    fun destroy() {
        signallingClient.destroy()
    }
}
