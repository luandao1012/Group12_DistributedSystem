package com.example.chat_p2p_app.ui.contacts.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chat_p2p_app.databinding.ItemFriendBinding
import com.example.chat_p2p_app.model.User
import com.example.chat_p2p_app.repository.UserRepository
import com.example.chat_p2p_app.utils.AvatarUtils

class FriendsAdapter(
    private val onFriendClick: (User) -> Unit,
    private val onChatClick: (User) -> Unit,
    private val userRepository: UserRepository
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    private val friends = mutableListOf<User>()

    fun updateFriends(newFriends: List<User>) {
        friends.clear()
        friends.addAll(newFriends)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(friends[position])
    }

    override fun getItemCount(): Int = friends.size

    inner class FriendViewHolder(private val binding: ItemFriendBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: User) {
            binding.tvName.text = friend.displayName

            AvatarUtils.loadUserAvatar(
                context = itemView.context,
                imageView = binding.ivAvatar,
                photoUrl = friend.photoUrl,
                userId = friend.uid,
                isCircular = true
            )

            binding.root.setOnClickListener {
                onFriendClick(friend)
            }
            binding.btnChat.setOnClickListener {
                onChatClick(friend)
            }
        }
    }
} 