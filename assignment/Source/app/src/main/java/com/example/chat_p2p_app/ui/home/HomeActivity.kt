package com.example.chat_p2p_app.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat_p2p_app.common.ConstValue.TAG_CURRENT_USER
import com.example.chat_p2p_app.common.ConstValue.TAG_TARGET_USER
import com.example.chat_p2p_app.database.entity.ChatHistoryEntity
import com.example.chat_p2p_app.databinding.ActivityHomeBinding
import com.example.chat_p2p_app.model.User
import com.example.chat_p2p_app.repository.UserRepository
import com.example.chat_p2p_app.ui.auth.AuthViewModel
import com.example.chat_p2p_app.ui.chat.ChatActivity
import com.example.chat_p2p_app.ui.contacts.ContactsActivity
import com.example.chat_p2p_app.ui.home.adapter.ChatHistoryAdapter
import com.example.chat_p2p_app.utils.AvatarUtils
import com.example.chat_p2p_app.utils.setPaddingEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private var _binding: ActivityHomeBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var chatHistoryAdapter: ChatHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setPaddingEdgeToEdge(binding)
        handleBackPress()
        setupUI()
        setupRecyclerView()
        observeUser()
        observeChatHistories()
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.refreshChatHistories()
    }

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun setupUI() {
        binding.fabContacts.setOnClickListener {
            val intent = Intent(this, ContactsActivity::class.java)
            startActivity(intent)
        }

        binding.ivUserAvatar.setOnClickListener {
            showUserProfileBottomSheet()
        }
    }

    private fun showUserProfileBottomSheet() {
        val bottomSheet = UserProfileBottomSheetDialogFragment.newInstance()
        bottomSheet.show(supportFragmentManager, UserProfileBottomSheetDialogFragment.TAG)
    }

    private fun setupRecyclerView() {
        chatHistoryAdapter = ChatHistoryAdapter(
            onChatClick = { chatHistory ->
                openChatWithUser(chatHistory)
            },
            onChatLongClick = { chatHistory ->
                Toast.makeText(this, "Long clicked on ${chatHistory.otherUserName}", Toast.LENGTH_SHORT).show()
            },
            userRepository = userRepository
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = chatHistoryAdapter
        }
    }

    private fun openChatWithUser(chatHistory: ChatHistoryEntity) {
        lifecycleScope.launch {
            authViewModel.currentUser.collect { currentUser ->
                if (currentUser != null) {
                    val targetUser = User(
                        uid = chatHistory.otherUserId,
                        displayName = chatHistory.otherUserName,
                        photoUrl = chatHistory.otherUserPhotoUrl,
                        email = ""
                    )

                    val intent = Intent(this@HomeActivity, ChatActivity::class.java).apply {
                        putExtra(TAG_CURRENT_USER, currentUser)
                        putExtra(TAG_TARGET_USER, targetUser)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun observeUser() {
        lifecycleScope.launch {
            authViewModel.currentUser.collect { user ->
                user?.let {
                    AvatarUtils.loadUserAvatar(
                        context = this@HomeActivity,
                        imageView = binding.ivUserAvatar,
                        photoUrl = it.photoUrl,
                        userId = it.uid,
                        isCircular = true
                    )
                } ?: run {
                    AvatarUtils.loadUserAvatar(
                        context = this@HomeActivity,
                        imageView = binding.ivUserAvatar,
                        photoUrl = null,
                        userId = "default_user",
                        isCircular = true
                    )
                }
            }
        }
    }

    private fun observeChatHistories() {
        lifecycleScope.launch {
            homeViewModel.chatHistories.collect { histories ->
                chatHistoryAdapter.updateChatHistories(histories)
            }
        }

        lifecycleScope.launch {
            homeViewModel.errorMessage.collect { error ->
                Toast.makeText(this@HomeActivity, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}