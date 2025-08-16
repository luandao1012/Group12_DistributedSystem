package com.example.chat_p2p_app.model

import com.google.firebase.Timestamp

data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val fromUserName: String = "",
    val toUserName: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", "", "", "", RequestStatus.PENDING, Timestamp.now(), Timestamp.now())
}

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

data class Friendship(
    val id: String = "",
    val user1Id: String = "",
    val user2Id: String = "",
    val user1Name: String = "",
    val user2Name: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val lastConnectedAt: Timestamp? = null
) {
    constructor() : this("", "", "", "", "", Timestamp.now(), null)
}