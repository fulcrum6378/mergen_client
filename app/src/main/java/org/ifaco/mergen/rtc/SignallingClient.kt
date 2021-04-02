package org.ifaco.mergen.rtc

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.*
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.ifaco.mergen.Panel
import org.ifaco.mergen.Panel.Companion.handler
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
class SignallingClient(
    private val listener: SignallingClientListener,
    address: String
) : CoroutineScope {
    private val job = Job()
    private val gson = Gson()
    override val coroutineContext = Dispatchers.IO + job
    private val sendChannel = ConflatedBroadcastChannel<String>()
    private val client = HttpClient(CIO) {
        install(WebSockets)
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    init {
        address.split(":").apply { connect(this[0], this[1].toInt()) }
    }

    private fun connect(ip: String, port: Int) = launch {
        client.ws(host = ip, port = port, path = "") {
            listener.onConnectionEstablished()
            val sendData = sendChannel.openSubscription()
            try {
                while (true) {
                    sendData.poll()?.let {
                        handler?.obtainMessage(Panel.Action.SOCKET.ordinal, "Sending: $it")?.sendToTarget()
                        outgoing.send(Frame.Text(it))
                    }
                    incoming.poll()?.let { frame ->
                        if (frame !is Frame.Text) return@let
                        val data = frame.readText()
                        handler?.obtainMessage(Panel.Action.SOCKET.ordinal, data)?.sendToTarget()
                        val jsonObject = gson.fromJson(data, JsonObject::class.java)
                        withContext(Dispatchers.Main) {
                            if (jsonObject.has("serverUrl"))
                                listener.onIceCandidateReceived(
                                    gson.fromJson(jsonObject, IceCandidate::class.java)
                                )
                            else if (jsonObject.has("type") && jsonObject.get("type").asString == "OFFER")
                                listener.onOfferReceived(
                                    gson.fromJson(jsonObject, SessionDescription::class.java)
                                )
                            else if (jsonObject.has("type") && jsonObject.get("type").asString == "ANSWER")
                                listener.onAnswerReceived(
                                    gson.fromJson(jsonObject, SessionDescription::class.java)
                                )
                        }

                    }
                }
            } catch (exception: Throwable) {
                handler?.obtainMessage(Panel.Action.SOCKET.ordinal, "asd")?.sendToTarget()
            }
        }
    }

    fun send(dataObject: Any?) = runBlocking {
        sendChannel.send(gson.toJson(dataObject))
    }

    fun destroy() {
        client.close()
        job.complete()
    }
}
