package com.example.chat_p2p_app.ui.contacts.allusers

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
import com.example.chat_p2p_app.databinding.FragmentAllUsersBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AllUsersFragment : Fragment() {
    private var _binding: FragmentAllUsersBinding? = null
    private val binding get() = _binding!!
    private lateinit var allUsersAdapter: AllUsersAdapter
    private val viewModel: AllUsersViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        allUsersAdapter = AllUsersAdapter(
            onUserClick = { user ->
                Toast.makeText(context, "Clicked on ${user.displayName}", Toast.LENGTH_SHORT).show()
            },
            onAddFriendClick = { user ->
                viewModel.sendFriendRequest(user)
            }
        )
        binding.rvAllUsers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = allUsersAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearchAllUsers.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchUsers(s?.toString() ?: "")
            }
        })
    }

    private fun searchUsers(query: String) {
        viewModel.searchUsers(query)
    }

    private fun observeViewModel() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            allUsersAdapter.updateUsers(users)
            showEmptyState(users.isEmpty())
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.friendRequestSent.observe(viewLifecycleOwner) { sent ->
            if (sent) {
                Toast.makeText(context, "Friend request sent successfully!", Toast.LENGTH_SHORT).show()
                viewModel.clearFriendRequestSent()
            }
        }
    }

    private fun showEmptyState(show: Boolean) {
        binding.tvEmptyAllUsers.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvAllUsers.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 