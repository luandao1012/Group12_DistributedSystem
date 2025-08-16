package com.example.chat_p2p_app.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chat_p2p_app.database.entity.ChatHistoryEntity
import com.example.chat_p2p_app.databinding.ItemChatHistoryBinding
import com.example.chat_p2p_app.repository.UserRepository
import com.example.chat_p2p_app.utils.AvatarUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatHistoryAdapter(
    private val onChatClick: (ChatHistoryEntity) -> Unit,
    private val onChatLongClick: (ChatHistoryEntity) -> Unit = {},
    private val userRepository: UserRepository
) : RecyclerView.Adapter<ChatHistoryAdapter.ChatHistoryViewHolder>() {

    private val chatHistories = mutableListOf<ChatHistoryEntity>()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun updateChatHistories(newChatHistories: List<ChatHistoryEntity>) {
        chatHistories.clear()
        chatHistories.addAll(newChatHistories)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatHistoryViewHolder {
        val binding = ItemChatHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatHistoryViewHolder, position: Int) {
        holder.bind(chatHistories[position])
    }

    override fun getItemCount(): Int = chatHistories.size

    inner class ChatHistoryViewHolder(
        private val binding: ItemChatHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chatHistory: ChatHistoryEntity) {
            binding.apply {
                tvUserName.text = chatHistory.otherUserName

                tvLastMessage.text = chatHistory.lastMessage

                val timestamp = Date(chatHistory.lastMessageTimestamp)
                val now = Date()
                val timeDiff = now.time - timestamp.time
                val daysDiff = timeDiff / (1000 * 60 * 60 * 24)

                tvTimestamp.text = when {
                    daysDiff == 0L -> timeFormat.format(timestamp)
                    daysDiff == 1L -> "Hôm qua"
                    daysDiff < 7 -> "${daysDiff} ngày trước"
                    else -> dateFormat.format(timestamp)
                }



                AvatarUtils.loadUserAvatar(
                    context = itemView.context,
                    imageView = ivUserAvatar,
                    photoUrl = chatHistory.otherUserPhotoUrl,
                    userId = chatHistory.otherUserId,
                    isCircular = true
                )

                root.setOnClickListener {
                    onChatClick(chatHistory)
                }

                root.setOnLongClickListener {
                    onChatLongClick(chatHistory)
                    true
                }
            }
        }
    }
}
