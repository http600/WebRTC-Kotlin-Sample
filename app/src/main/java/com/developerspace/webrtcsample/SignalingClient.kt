package com.developerspace.webrtcsample

import android.util.Log
import com.google.gson.Gson
import com.parse.ParseObject
import com.parse.ParseQuery
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
class SignalingClient(
    private val meetingID: String,
    private val listener: SignalingClientListener
) : CoroutineScope {

    private val job = Job()

    val TAG = "SignallingClient"

    var SDPtype: String? = null
    override val coroutineContext = Dispatchers.IO + job

    private val sendChannel = ConflatedBroadcastChannel<String>()

    init {
        connect()
    }

    private fun connect() = launch {
        listener.onConnectionEstablished()
        val sendData = sendChannel.offer("")
        sendData.let {
            Log.v(this@SignalingClient.javaClass.simpleName, "Sending: $it")
        }
        try {
            val parseQuery = ParseQuery<ParseObject>("calls2021")
            parseQuery.whereEqualTo("key", meetingID)
            parseQuery.findInBackground { objects, e ->
                run {
                    if (null != e) {
                        Log.w(TAG, "parse x, findInBackground error: " + Gson().toJson(e))
                        return@findInBackground
                    }
                    Log.v(TAG, "parse x, findInBackground: " + Gson().toJson(objects))
                    if (objects.isEmpty()) return@findInBackground
                    val payload = objects[0]
                    if (!payload.containsKey("type")) return@findInBackground
                    when {
                        payload.containsKey("sdp") && payload.getString("type") == "OFFER" -> {
                            listener.onOfferReceived(
                                SessionDescription(
                                    SessionDescription.Type.OFFER,
                                    payload.getString("sdp").toString()
                                )
                            )
                            SDPtype = "Offer"
                        }
                        payload.containsKey("sdp") && payload.getString("type") == "ANSWER" -> {
                            listener.onAnswerReceived(
                                SessionDescription(
                                    SessionDescription.Type.ANSWER,
                                    payload.getString("sdp").toString()
                                )
                            )
                            SDPtype = "Answer"
                        }
                        payload.getString("type") == "END_CALL" -> {
                            listener.onCallEnded()
                            SDPtype = "End Call"
                        }
                    }
                }
            }
            val parseQueryCall = ParseQuery<ParseObject>("calls2021")
            parseQueryCall.whereEqualTo("key", meetingID)
            parseQueryCall.findInBackground { objects, e ->
                run {
                    if (null != e) return@findInBackground
                    if (objects.isEmpty()) return@findInBackground
                    val call = objects[0]
                    val parseQueryCandidate = ParseQuery<ParseObject>("candidates2021")
                    parseQueryCandidate.whereEqualTo("call", call)
                    parseQueryCandidate.findInBackground { objects, e ->
                        run {
                            if (null != e) return@findInBackground
                            if (objects.isEmpty()) return@findInBackground
                            val parseObject = objects[0]

                            if (SDPtype == "Offer" && parseObject.containsKey("type") && parseObject.getString(
                                    "type"
                                ) == "offerCandidate"
                            ) {
                                listener.onIceCandidateReceived(
                                    IceCandidate(
                                        parseObject.getString("sdpMid").toString(),
                                        Math.toIntExact(parseObject.getLong("sdpMLineIndex")),
                                        parseObject.getString("sdpCandidate").toString()
                                    )
                                )
                            } else if (SDPtype == "Answer" && parseObject.containsKey("type") && parseObject.getString(
                                    "type"
                                ) == "answerCandidate"
                            ) {
                                listener.onIceCandidateReceived(
                                    IceCandidate(
                                        parseObject.getString("sdpMid").toString(),
                                        Math.toIntExact(parseObject.getLong("sdpMLineIndex")),
                                        parseObject.getString("sdpCandidate").toString()
                                    )
                                )
                            }
                        }
                    }
                    Log.v(TAG, "findInBackground: " + Gson().toJson(call))
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "connectException: $exception")
        }
    }

    fun sendIceCandidate(candidate: IceCandidate?, isJoin: Boolean) = runBlocking {
        val type = when {
            isJoin -> "answerCandidate"
            else -> "offerCandidate"
        }
        val candidateConstant = hashMapOf(
            "serverUrl" to candidate?.serverUrl,
            "sdpMid" to candidate?.sdpMid,
            "sdpMLineIndex" to candidate?.sdpMLineIndex,
            "sdpCandidate" to candidate?.sdp,
            "type" to type
        )
        val parseObjectCandidate = ParseObject("candidates2021")
        parseObjectCandidate.put("key", type)
        candidateConstant.forEach { (k, v) ->
            parseObjectCandidate.put(
                k,
                if (v is String) v else Gson().toJson(v)
            )
        }
        val parseQueryCall = ParseQuery<ParseObject>("calls2021")
        parseQueryCall.whereEqualTo("key", meetingID)
        parseQueryCall.findInBackground { objects, e ->
            run {
                if (null != e) return@findInBackground
                if (objects.isEmpty()) return@findInBackground
                val call = objects[0]
                parseObjectCandidate.put("call", call)
                parseObjectCandidate.saveInBackground().onSuccessTask {
                    Log.v(TAG, "parse x, saveInBackground: " + Gson().toJson(parseObjectCandidate))
                    return@onSuccessTask it
                }
            }
        }
    }

    fun destroy() {
        job.complete()
    }
}
