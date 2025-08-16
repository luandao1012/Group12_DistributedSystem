package com.example.chat_p2p_app.ui.contacts.requests

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chat_p2p_app.databinding.ItemFriendRequestBinding
import com.example.chat_p2p_app.model.FriendRequest
import com.example.chat_p2p_app.utils.AvatarUtils

class FriendRequestsAdapter(
    private val onAcceptClick: (FriendRequest) -> Unit,
    private val onRejectClick: (FriendRequest) -> Unit
) : RecyclerView.Adapter<FriendRequestsAdapter.RequestViewHolder>() {

    private val requests = mutableListOf<FriendRequest>()

    fun updateRequests(newRequests: List<FriendRequest>) {
        requests.clear()
        requests.addAll(newRequests)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemFriendRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    override fun getItemCount(): Int = requests.size

    inner class RequestViewHolder(
        private val binding: ItemFriendRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(request: FriendRequest) {
            binding.tvName.text = request.fromUserName

            AvatarUtils.loadUserAvatar(
                context = itemView.context,
                imageView = binding.ivAvatar,
                photoUrl = null,
                userId = request.fromUserId,
                isCircular = true
            )

            binding.btnAccept.setOnClickListener {
                onAcceptClick(request)
            }

            binding.btnReject.setOnClickListener {
                onRejectClick(request)
            }
        }
    }
} 