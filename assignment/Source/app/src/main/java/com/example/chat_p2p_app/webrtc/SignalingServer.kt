package com.example.chat_p2p_app.webrtc

import android.util.Log
import com.example.chat_p2p_app.common.ConstValue.TAG
import com.example.chat_p2p_app.model.connection.Role
import com.example.chat_p2p_app.model.connection.RoomMeta
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import org.webrtc.IceCandidate

class SignalingServer(private val db: FirebaseFirestore) {

    private var answerListener: ListenerRegistration? = null
    private var offerListener: ListenerRegistration? = null
    private var candidatesListener: ListenerRegistration? = null
    private var joinedStatusListener: ListenerRegistration? = null
    private val collectionDb by lazy { db.collection("rooms") }

    fun roomIdFor(uidA: String, uidB: String): String {
        val (low, high) = listOf(uidA, uidB).sorted()
        return "${low}_${high}"
    }

    fun ensureRoomAndResolveRole(
        myUid: String,
        peerUid: String,
        onSuccess: (RoomMeta) -> Unit,
        onError: (String) -> Unit
    ) {
        val roomId = roomIdFor(myUid, peerUid)
        val roomRef = collectionDb.document(roomId)
        roomRef.get(Source.SERVER)
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    val data = mapOf(
                        "participants" to listOf(myUid, peerUid).sorted(),
                        "offerBy" to myUid,
                        "status" to "INIT",
                        "joinedUsers" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    roomRef.set(data, SetOptions.merge())
                        .addOnSuccessListener { onSuccess(RoomMeta(roomId, Role.CALLER)) }
                        .addOnFailureListener { onError(it.message ?: "Failed to create room") }
                    return@addOnSuccessListener
                }

                val offer = snap.getString("offer")
                val offerBy = snap.getString("offerBy")
                val answer = snap.getString("answer")

                val role = when {
                    offer == null -> {
                        if (offerBy == null) {
                            roomRef.update("offerBy", myUid)
                        }
                        Role.CALLER
                    }

                    answer == null && offerBy != myUid -> {
                        Role.ANSWER
                    }

                    else -> {
                        if (offerBy == myUid) Role.CALLER else Role.ANSWER
                    }
                }
                onSuccess(RoomMeta(roomId, role))
            }
            .addOnFailureListener { onError(it.message ?: "Failed to get room from server") }
    }

    fun setOffer(
        roomId: String,
        offerSdp: String,
        offerByUid: String,
        onSuccess: (Boolean, String?) -> Unit
    ) {
        collectionDb.document(roomId)
            .set(
                mapOf(
                    "offer" to offerSdp,
                    "offerBy" to offerByUid,
                    "status" to "WAITING_ANSWER",
                    "offerAt" to FieldValue.serverTimestamp()
                ), SetOptions.merge()
            )
            .addOnSuccessListener { onSuccess(true, null) }
            .addOnFailureListener { onSuccess(false, it.message) }
    }

    fun setAnswer(roomId: String, answerSdp: String, answerByUid: String, onSuccess: (Boolean, String?) -> Unit) {
        collectionDb.document(roomId)
            .set(
                mapOf(
                    "answer" to answerSdp,
                    "answerBy" to answerByUid,
                    "status" to "ANSWERED",
                    "answerAt" to FieldValue.serverTimestamp()
                ), SetOptions.merge()
            )
            .addOnSuccessListener { onSuccess(true, null) }
            .addOnFailureListener { onSuccess(false, it.message) }
    }

    fun findOffer(roomId: String, onOffer: (String) -> Unit) {
        val ref = collectionDb.document(roomId)
        ref.get().addOnSuccessListener { snap ->
            val offer = snap.getString("offer")
            if (offer != null) {
                onOffer(offer)
            } else {
                offerListener?.remove()
                offerListener = ref.addSnapshotListener { s, _ ->
                    val oo = s?.getString("offer") ?: return@addSnapshotListener
                    onOffer(oo)
                    offerListener?.remove()
                    offerListener = null
                }
            }
        }
    }

    fun listenAnswer(roomId: String, onAnswer: (String) -> Unit) {
        val ref = collectionDb.document(roomId)
        answerListener?.remove()
        answerListener = ref.addSnapshotListener { snap, _ ->
            val ans = snap?.getString("answer") ?: return@addSnapshotListener
            answerListener?.remove()
            answerListener = null
            onAnswer(ans)
        }
    }

    fun addLocalIceCandidate(roomId: String, fromUid: String, c: IceCandidate) {
        collectionDb.document(roomId)
            .collection("candidates")
            .add(
                mapOf(
                    "fromUid" to fromUid,
                    "sdpMid" to c.sdpMid,
                    "sdpMLineIndex" to c.sdpMLineIndex,
                    "candidate" to c.sdp,
                    "ts" to FieldValue.serverTimestamp()
                )
            )
    }

    fun listenRemoteCandidates(roomId: String, myUid: String, onCandidate: (IceCandidate) -> Unit) {
        candidatesListener?.remove()
        candidatesListener = collectionDb.document(roomId)
            .collection("candidates")
            .addSnapshotListener { snap, _ ->
                snap?.documentChanges?.forEach { ch ->
                    val data = ch.document.data
                    if (data["fromUid"] == myUid) return@forEach
                    onCandidate(
                        IceCandidate(
                            data["sdpMid"] as String,
                            (data["sdpMLineIndex"] as Number).toInt(),
                            data["candidate"] as String
                        )
                    )
                }
            }
    }

    fun setUserJoined(roomId: String, userId: String, onSuccess: (Boolean, String?) -> Unit) {
        val roomRef = collectionDb.document(roomId)
        db.runTransaction { txn ->
            val snap = txn.get(roomRef)
            if (snap.exists()) {
                val currentJoined = snap.get("joinedUsers") as? List<String> ?: emptyList()
                if (!currentJoined.contains(userId)) {
                    val updatedJoined = currentJoined + userId
                    txn.update(roomRef, "joinedUsers", updatedJoined)
                }
            }
        }
            .addOnSuccessListener { onSuccess(true, null) }
            .addOnFailureListener { onSuccess(false, it.message) }
    }

    fun setUserLeft(roomId: String, userId: String, onSuccess: ((Boolean, String?) -> Unit)? = null) {
        val roomRef = collectionDb.document(roomId)
        db.runTransaction { txn ->
            val snap = txn.get(roomRef)
            if (snap.exists()) {
                val currentJoined = snap.get("joinedUsers") as? List<String> ?: emptyList()
                val updatedJoined = currentJoined.filter { it != userId }

                if (updatedJoined.isEmpty()) {
                    txn.delete(roomRef)
                } else {
                    txn.update(roomRef, "joinedUsers", updatedJoined)
                }
            }
        }
            .addOnSuccessListener {
                onSuccess?.invoke(true, null)
                deleteRoomCandidates(roomId)
            }
            .addOnFailureListener { onSuccess?.invoke(false, it.message) }
    }

    fun listenJoinedStatus(roomId: String, onJoinedUsersChanged: (List<String>) -> Unit) {
        joinedStatusListener?.remove()
        joinedStatusListener = collectionDb.document(roomId)
            .addSnapshotListener { snap, _ ->
                val joinedUsers = snap?.get("joinedUsers") as? List<String> ?: emptyList()
                onJoinedUsersChanged(joinedUsers)
            }
    }

    fun deleteRoomCandidates(roomId: String) {
        val candidatesCol = collectionDb.document(roomId).collection("candidates")
        candidatesCol.get().addOnSuccessListener { snap ->
            val batch = db.batch()
            snap.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().addOnSuccessListener {
                Log.d(TAG, "Deleted candidates of room $roomId")
            }.addOnFailureListener {
                Log.e(TAG, "Failed to delete candidates of room $roomId", it)
            }
        }
    }

    fun dispose() {
        answerListener?.remove()
        answerListener = null
        offerListener?.remove()
        offerListener = null
        candidatesListener?.remove()
        candidatesListener = null
        joinedStatusListener?.remove()
        joinedStatusListener = null
    }
}
