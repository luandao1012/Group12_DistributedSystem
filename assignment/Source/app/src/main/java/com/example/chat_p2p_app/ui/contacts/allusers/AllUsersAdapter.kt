package com.example.chat_p2p_app.ui.contacts.allusers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chat_p2p_app.databinding.ItemAllUserBinding
import com.example.chat_p2p_app.model.User
import com.example.chat_p2p_app.utils.AvatarUtils

class AllUsersAdapter(
    private val onUserClick: (User) -> Unit,
    private val onAddFriendClick: (User) -> Unit
) : RecyclerView.Adapter<AllUsersAdapter.UserViewHolder>() {

    private val users = mutableListOf<User>()

    fun updateUsers(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemAllUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(
        private val binding: ItemAllUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvName.text = user.displayName

            AvatarUtils.loadUserAvatar(
                context = itemView.context,
                imageView = binding.ivAvatar,
                photoUrl = user.photoUrl,
                userId = user.uid,
                isCircular = true
            )

            binding.root.setOnClickListener {
                onUserClick(user)
            }

            binding.btnAddFriend.setOnClickListener {
                onAddFriendClick(user)
            }
        }
    }
} 