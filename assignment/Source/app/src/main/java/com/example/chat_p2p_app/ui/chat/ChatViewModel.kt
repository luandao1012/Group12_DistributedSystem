package com.example.chat_p2p_app.ui.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_p2p_app.common.ConstValue
import com.example.chat_p2p_app.model.ChatMessage
import com.example.chat_p2p_app.model.MessageType
import com.example.chat_p2p_app.model.User
import com.example.chat_p2p_app.model.connection.Role
import com.example.chat_p2p_app.repository.ChatHistoryRepository
import com.example.chat_p2p_app.webrtc.SignalingServer
import com.example.chat_p2p_app.webrtc.WebRTCManager
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    firestore: FirebaseFirestore,
    @ApplicationContext appContext: Context,
    private val chatHistoryRepository: ChatHistoryRepository
) : ViewModel(), WebRTCManager.Listener {

    companion object {
        private const val TAG = ConstValue.TAG
    }

    private var currentUser: User? = null
    private var targetUser: User? = null
    private var currentRoomId = ""
    private val signaling = SignalingServer(firestore)
    private val rtc = WebRTCManager(appContext, this)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _successMessage = MutableSharedFlow<String>()
    val successMessage: SharedFlow<String> = _successMessage.asSharedFlow()

    private val _connectionStatus = MutableStateFlow("Connecting...")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private var lastJoinedCount = 0

    fun setUserIds(currentUser: User, targetUser: User) {
        this.currentUser = currentUser
        this.targetUser = targetUser
        loadChatHistory()
    }

    private fun loadChatHistory() {
        val current = currentUser ?: return
        val target = targetUser ?: return

        viewModelScope.launch {
            try {
                val existingMessages = chatHistoryRepository.getConversationMessages(current.uid, target.uid)
                _messages.value = existingMessages
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chat history", e)
            }
        }
    }

    fun openChatWithPeer() {
        val myUid = currentUser?.uid ?: return
        val peerUid = targetUser?.uid ?: return

        signaling.ensureRoomAndResolveRole(
            myUid, peerUid,
            onSuccess = { meta ->
                currentRoomId = meta.roomId
                signaling.setUserJoined(meta.roomId, myUid) { _, _ -> }
                signaling.listenJoinedStatus(meta.roomId) { joinedUsers ->
                    updateConnectionStatus(joinedUsers, myUid, peerUid)
                }
                if (meta.role == Role.CALLER) startCallerFlow(meta.roomId)
                else startCalleeFlow(meta.roomId)
            },
            onError = { _errorMessage.tryEmit("Error opening chat: $it") }
        )
    }

    private fun startCallerFlow(roomId: String) {
        val myUid = currentUser?.uid ?: return
        rtc.createPeer(true)
        rtc.createOffer { offerSdp ->
            signaling.setOffer(roomId, offerSdp, myUid) { ok, err ->
                if (!ok) _errorMessage.tryEmit("Failed to set offer: $err")
            }
            signaling.listenAnswer(roomId) {
                rtc.setRemoteAnswer(it) { ok ->
                    if (!ok) _errorMessage.tryEmit("Failed to set remote answer")
                }
            }
            signaling.listenRemoteCandidates(roomId, myUid) { rtc.addRemoteIce(it) }
        }
    }

    private fun startCalleeFlow(roomId: String) {
        val myUid = currentUser?.uid ?: return
        signaling.findOffer(roomId) { offer ->
            rtc.createPeer(false)
            rtc.setRemoteOffer(offer) {
                rtc.createAnswer { answerSdp ->
                    signaling.setAnswer(roomId, answerSdp, myUid) { _, _ -> }
                }
            }
            signaling.listenRemoteCandidates(roomId, myUid) { rtc.addRemoteIce(it) }
        }
    }

    fun sendMessage(messageText: String) {
        val current = currentUser ?: return
        val target = targetUser ?: return
        if (!rtc.sendMessage(messageText)) return
        val chatMessage = ChatMessage(
            UUID.randomUUID().toString(), current.uid, current.displayName,
            messageText, Date(), MessageType.TEXT, isFromMe = true
        )
        _messages.value = _messages.value + chatMessage
        viewModelScope.launch {
            chatHistoryRepository.saveChatMessage(
                senderId = current.uid,
                senderName = current.displayName,
                receiverId = target.uid,
                message = messageText,
                messageType = MessageType.TEXT
            )
        }
    }

    private fun addReceivedMessage(messageText: String) {
        val current = currentUser ?: return
        val target = targetUser ?: return

        val chatMessage = ChatMessage(
            UUID.randomUUID().toString(), target.uid, target.displayName,
            messageText, Date(), MessageType.TEXT, isFromMe = false
        )
        _messages.value = _messages.value + chatMessage

        viewModelScope.launch {
            chatHistoryRepository.saveChatMessage(
                senderId = target.uid,
                senderName = target.displayName,
                receiverId = current.uid,
                message = messageText,
                messageType = MessageType.TEXT
            )
        }
    }

    private fun updateConnectionStatus(joinedUsers: List<String>, myUid: String, peerUid: String) {
        val nowCount = joinedUsers.size
        if (joinedUsers.contains(myUid) && !joinedUsers.contains(peerUid) && lastJoinedCount == 2) {
            resetRoomAndStartOver()
        }
        lastJoinedCount = nowCount
        _connectionStatus.value = when {
            joinedUsers.contains(myUid) && joinedUsers.contains(peerUid) -> "Connected"
            else -> "Connecting..."
        }
    }

    private fun resetRoomAndStartOver() {
        val myUid = currentUser?.uid ?: return
        val peerUid = targetUser?.uid ?: return
        val oldRoomId = currentRoomId
        signaling.setUserLeft(oldRoomId, myUid) { _, _ ->
            signaling.ensureRoomAndResolveRole(
                myUid, peerUid,
                onSuccess = { meta ->
                    currentRoomId = meta.roomId
                    signaling.setUserJoined(meta.roomId, myUid) { _, _ -> }
                    signaling.listenJoinedStatus(meta.roomId) { joinedUsers ->
                        updateConnectionStatus(joinedUsers, myUid, peerUid)
                    }
                    startCallerFlow(meta.roomId)
                },
                onError = { Log.e(TAG, "Error re-creating room: $it") }
            )
        }
    }

    fun leaveRoom() {
        val uid = currentUser?.uid ?: return
        if (currentRoomId.isNotEmpty()) {
            signaling.setUserLeft(currentRoomId, uid) { _, _ -> }
        }
    }

    override fun onCleared() {
        leaveRoom()
        _messages.value = emptyList()
        currentUser = null
        targetUser = null
        signaling.dispose()
        rtc.close()
    }

    override fun onLocalIceCandidate(candidate: IceCandidate) {
        val myUid = currentUser?.uid ?: return
        signaling.addLocalIceCandidate(currentRoomId, myUid, candidate)
    }

    override fun onConnectionStateChanged(state: PeerConnection.PeerConnectionState) {
        Log.d(TAG, "Connection state changed: $state")
    }

    override fun onMessageReceived(text: String) {
        addReceivedMessage(text)
    }

    override fun onDataChannelStateChanged(state: DataChannel.State) {
        Log.d(TAG, "Data channel state changed: $state")
    }

    override fun onError(message: String) {
        Log.e(TAG, "Error: $message")
    }
}
