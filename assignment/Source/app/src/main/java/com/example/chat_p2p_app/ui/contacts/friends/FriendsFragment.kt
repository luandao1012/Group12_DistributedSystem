package com.example.chat_p2p_app.ui.contacts.friends

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat_p2p_app.common.ConstValue.TAG_CURRENT_USER
import com.example.chat_p2p_app.common.ConstValue.TAG_TARGET_USER
import com.example.chat_p2p_app.databinding.FragmentFriendsBinding
import com.example.chat_p2p_app.model.User
import com.example.chat_p2p_app.ui.chat.ChatActivity
import com.example.chat_p2p_app.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FriendsFragment : Fragment() {
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!
    private lateinit var friendsAdapter: FriendsAdapter
    private val viewModel: FriendsViewModel by viewModels()

    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        friendsAdapter = FriendsAdapter(
            onFriendClick = { friend ->
                startChatWithFriend(friend)
            },
            onChatClick = { friend ->
            },
            userRepository = userRepository
        )
        binding.rvFriends.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = friendsAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearchFriends.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchFriends(s?.toString() ?: "")
            }
        })
    }

    private fun searchFriends(query: String) {
        viewModel.searchFriends(query)
    }

    private fun observeViewModel() {
        viewModel.friends.observe(viewLifecycleOwner) { friends ->
            friendsAdapter.updateFriends(friends)
            showEmptyState(friends.isEmpty())
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun showEmptyState(show: Boolean) {
        binding.tvEmptyFriends.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvFriends.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun startChatWithFriend(friend: User) {
        viewModel.getCurrentUser()?.let { currentUser ->
            val intent = Intent(this.activity, ChatActivity::class.java)
            val bundle = Bundle().apply {
                putParcelable(TAG_CURRENT_USER, currentUser)
                putParcelable(TAG_TARGET_USER, friend)
            }
            intent.putExtras(bundle)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 