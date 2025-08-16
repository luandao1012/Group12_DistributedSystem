package com.example.chat_p2p_app.webrtc

import android.content.Context
import android.util.Log
import com.example.chat_p2p_app.common.ConstValue.TAG
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.nio.ByteBuffer

class WebRTCManager(
    private val context: Context,
    private val listener: Listener
) {

    interface Listener {
        fun onLocalIceCandidate(candidate: IceCandidate)
        fun onConnectionStateChanged(state: PeerConnection.PeerConnectionState)
        fun onMessageReceived(text: String)
        fun onDataChannelStateChanged(state: DataChannel.State)
        fun onError(message: String)
    }

    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var eglBase: EglBase
    private var factory: PeerConnectionFactory

    private val iceServers: List<PeerConnection.IceServer> = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )
        eglBase = EglBase.create()

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun createPeer(createLocalDataChannel: Boolean) {
        peerConnection = factory.createPeerConnection(iceServers, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                Log.e(TAG, "onIceCandidate: $candidate, listener: $listener")
                listener.onLocalIceCandidate(candidate)
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                Log.e(TAG, "onConnectionChange: $newState, listener: $listener")
                listener.onConnectionStateChanged(newState)
            }

            override fun onIceConnectionReceivingChange(p0: Boolean) {
            }

            override fun onDataChannel(dc: DataChannel) {
                dataChannel = dc
                attachDcObserver(dc)
            }

            override fun onSignalingChange(p0: PeerConnection.SignalingState) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>) {}
            override fun onAddStream(p0: MediaStream) {}
            override fun onRemoveStream(p0: MediaStream) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver, p1: Array<out MediaStream>) {}
        }) ?: run {
            listener.onError("Cannot create PeerConnection")
            return
        }

        if (createLocalDataChannel) {
            dataChannel = peerConnection!!.createDataChannel("chat", DataChannel.Init()).also { attachDcObserver(it) }
        }
    }

    private fun attachDcObserver(dc: DataChannel) {
        dc.registerObserver(object : DataChannel.Observer {
            override fun onMessage(buffer: DataChannel.Buffer) {
                val bytes = ByteArray(buffer.data.remaining())
                buffer.data.get(bytes)
                listener.onMessageReceived(String(bytes))
            }

            override fun onBufferedAmountChange(p0: Long) {}
            override fun onStateChange() {
                listener.onDataChannelStateChanged(dc.state())
            }
        })
    }

    fun createOffer(onSdp: (String) -> Unit) {
        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(this, desc)
                onSdp(desc.description)
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {
                listener.onError("createOffer failed: $p0")
            }

            override fun onSetFailure(p0: String?) {}
        }, constraints) ?: listener.onError("Peer not created")
    }

    fun setRemoteOffer(offerSdp: String, onSetSuccess: (() -> Unit)? = null) {
        val desc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                onSetSuccess?.invoke()
            }

            override fun onSetFailure(p0: String?) {
                listener.onError("setRemoteOffer failed: $p0")
            }

            override fun onCreateSuccess(d: SessionDescription) {}
            override fun onCreateFailure(p0: String?) {}
        }, desc)
    }

    fun createAnswer(onSdp: (String) -> Unit) {
        val constraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(this, desc)
                onSdp(desc.description)
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {
                listener.onError("createAnswer failed: $p0")
            }

            override fun onSetFailure(p0: String?) {}
        }, constraints) ?: listener.onError("Peer not created")
    }

    fun setRemoteAnswer(answerSdp: String, onSuccess: ((Boolean) -> Unit)? = null) {
        val pc = peerConnection ?: return
        val desc = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        pc.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                onSuccess?.invoke(true)
            }

            override fun onSetFailure(p0: String?) {
                listener.onError("setRemoteAnswer failed: $p0")
                onSuccess?.invoke(false)
            }

            override fun onCreateSuccess(p0: SessionDescription) {}
            override fun onCreateFailure(p0: String?) {}
        }, desc)
    }


    fun addRemoteIce(candidate: IceCandidate) {
        Log.d(
            TAG,
            "addRemoteIce: $candidate, peerConnection: $peerConnection, pending: ${peerConnection?.remoteDescription}"
        )
        val pc = peerConnection ?: return
        pc.addIceCandidate(candidate)
    }

    fun sendMessage(text: String): Boolean {
        val dc = dataChannel ?: return false
        if (dc.state() != DataChannel.State.OPEN) return false
        val buf = DataChannel.Buffer(ByteBuffer.wrap(text.toByteArray()), false)
        dc.send(buf)
        return true
    }

    fun close() {
        dataChannel?.close()
        peerConnection?.close()
        dataChannel = null
        peerConnection = null
    }

}
