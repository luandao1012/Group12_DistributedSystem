package com.example.chat_p2p_app.ui.chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat_p2p_app.common.ConstValue.TAG
import com.example.chat_p2p_app.common.ConstValue.TAG_CURRENT_USER
import com.example.chat_p2p_app.common.ConstValue.TAG_TARGET_USER
import com.example.chat_p2p_app.databinding.ActivityChatBinding
import com.example.chat_p2p_app.model.User
import com.example.chat_p2p_app.repository.UserRepository
import com.example.chat_p2p_app.ui.chat.adapter.ChatAdapter
import com.example.chat_p2p_app.ui.home.HomeActivity
import com.example.chat_p2p_app.utils.AvatarUtils
import com.example.chat_p2p_app.utils.setPaddingEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {
    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var currentUser: User
    private lateinit var targetUser: User

    @Inject
    lateinit var userRepository: UserRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setPaddingEdgeToEdge(binding)
        handleBackPress()

        intent.extras?.let { dataBundle ->
            currentUser = dataBundle.getParcelable(TAG_CURRENT_USER, User::class.java) ?: run {
                Log.e(TAG, "Current user not found in intent")
                finish()
                return
            }

            targetUser = dataBundle.getParcelable(TAG_TARGET_USER, User::class.java) ?: run {
                Log.e(TAG, "Target user not found in intent")
                finish()
                return
            }
        }
        setupUI()
        setupObservers()
        initializeWebRTC()
    }

    private fun setupUI() {
        binding.tvTitleApp.text = targetUser.displayName

        AvatarUtils.loadUserAvatar(
            context = this,
            imageView = binding.ivUserAvatar,
            photoUrl = targetUser.photoUrl,
            userId = targetUser.uid,
            isCircular = true
        )

        lifecycleScope.launch {
        }

        chatAdapter = ChatAdapter()
        binding.rvMessages.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ChatActivity)
        }

        binding.btnSend.setOnClickListener {
            val message = binding.edtMessageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                chatViewModel.sendMessage(message)
                binding.edtMessageInput.text.clear()
            }
        }
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@ChatActivity, HomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
        })
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            chatViewModel.messages.collect { messages ->
                chatAdapter.updateMessages(messages)
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
            }
        }

        lifecycleScope.launch {
            chatViewModel.errorMessage.collect { message ->
                Toast.makeText(this@ChatActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            chatViewModel.successMessage.collect { message ->
                Toast.makeText(this@ChatActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            chatViewModel.connectionStatus.collect { status ->
                binding.tvConnectionStatus.text = status
                val isConnected = status == "Connected"
                binding.tvConnectionStatus.visibility = if (isConnected) GONE else VISIBLE
                updateOnlineStatus(isConnected)
            }
        }
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        if (isOnline) {
            binding.ivOnlineStatus.visibility = VISIBLE
            binding.tvStatus.text = "Online"
            binding.tvStatus.setTextColor(getColor(com.example.chat_p2p_app.R.color.success))
        } else {
            binding.ivOnlineStatus.visibility = GONE
            binding.tvStatus.text = "Offline"
            binding.tvStatus.setTextColor(getColor(com.example.chat_p2p_app.R.color.gray))
        }
    }

    private fun initializeWebRTC() {
        Log.d(TAG, "initializeWebRTC")
        chatViewModel.setUserIds(currentUser, targetUser)
        chatViewModel.openChatWithPeer()
    }

    override fun onDestroy() {
        super.onDestroy()
        chatViewModel.leaveRoom()
        _binding = null
    }
}
