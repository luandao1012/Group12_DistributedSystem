package com.example.chat_p2p_app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null
) : Parcelable {
    constructor() : this("", "", "", null)
}