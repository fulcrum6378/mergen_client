package org.ifaco.mergen.rtc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class SignallingClient(
    private val listener: SignallingClientListener,
    address: String
) : CoroutineScope {
    private val job = Job()
    override val coroutineContext = Dispatchers.IO + job

    /*private val client = HttpClient(CIO) {
        install(WebSockets)
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }*/

    private val sendChannel = ConflatedBroadcastChannel<String>()

    init {
        address.split(":").apply { connect(this[0], this[1].toInt()) }
    }

    private fun connect(ip: String, port: Int) = launch {
        /*client.ws(host = ip, port = port, path = "/") {
            listener.onConnectionEstablished()
            val sendData = sendChannel.openSubscription()
            try {
                while (true) {
                    sendData.poll()?.let {
                        Log.v(this@SignallingClient.javaClass.simpleName, "Sending: $it")
                        outgoing.send(Frame.Text(it))
                    }
                    incoming.poll()?.let { frame ->
                        if (frame is Frame.Text) {
                            val data = frame.readText()
                            Log.v(this@SignallingClient.javaClass.simpleName, "Received: $data")
                            val jsonObject = gson.fromJson(data, JsonObject::class.java)
                            withContext(Dispatchers.Main) {
                                if (jsonObject.has("serverUrl")) {
                                    listener.onIceCandidateReceived(
                                        gson.fromJson(
                                            jsonObject,
                                            IceCandidate::class.java
                                        )
                                    )
                                } else if (jsonObject.has("type") && jsonObject.get("type").asString == "OFFER") {
                                    listener.onOfferReceived(
                                        gson.fromJson(
                                            jsonObject,
                                            SessionDescription::class.java
                                        )
                                    )
                                } else if (jsonObject.has("type") && jsonObject.get("type").asString == "ANSWER") {
                                    listener.onAnswerReceived(
                                        gson.fromJson(
                                            jsonObject,
                                            SessionDescription::class.java
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (exception: Throwable) {
                Log.e("asd", "asd", exception)
            }
        }*/
    }

    fun send(dataObject: Any?) = runBlocking {
        //sendChannel.send(gson.toJson(dataObject))
    }

    fun destroy() {
        //client.close()
        job.complete()
    }
}
