package com.example.chat_p2p_app.ui.contacts.requests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat_p2p_app.databinding.FragmentFriendRequestsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FriendRequestsFragment : Fragment() {

    private var _binding: FragmentFriendRequestsBinding? = null
    private val binding get() = _binding!!
    private lateinit var requestsAdapter: FriendRequestsAdapter
    private val viewModel: FriendRequestsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        requestsAdapter = FriendRequestsAdapter(
            onAcceptClick = { request ->
                viewModel.acceptFriendRequest(request)
            },
            onRejectClick = { request ->
                viewModel.rejectFriendRequest(request)
            }
        )
        binding.rvFriendRequests.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = requestsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.requests.observe(viewLifecycleOwner) { requests ->
            requestsAdapter.updateRequests(requests)
            showEmptyState(requests.isEmpty())
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.requestAccepted.observe(viewLifecycleOwner) { accepted ->
            if (accepted) {
                Toast.makeText(context, "Friend request accepted!", Toast.LENGTH_SHORT).show()
                viewModel.clearRequestAccepted()
            }
        }

        viewModel.requestRejected.observe(viewLifecycleOwner) { rejected ->
            if (rejected) {
                Toast.makeText(context, "Friend request rejected!", Toast.LENGTH_SHORT).show()
                viewModel.clearRequestRejected()
            }
        }
    }

    private fun showEmptyState(show: Boolean) {
        binding.tvEmptyRequests.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvFriendRequests.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}