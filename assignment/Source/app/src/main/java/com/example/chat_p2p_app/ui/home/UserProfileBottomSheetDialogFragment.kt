package com.example.chat_p2p_app.ui.home

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.chat_p2p_app.R
import com.example.chat_p2p_app.utils.AvatarUtils
import com.example.chat_p2p_app.databinding.BottomSheetUserProfileBinding
import com.example.chat_p2p_app.model.AuthResult
import com.example.chat_p2p_app.ui.auth.AuthActivity
import com.example.chat_p2p_app.ui.auth.AuthViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserProfileBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetUserProfileBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeUser()
        observeAuthState()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        
        return dialog
    }

    private fun setupUI() {
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun observeUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.currentUser.collect { user ->
                user?.let {
                    binding.tvDisplayName.text = it.displayName.ifEmpty {
                        "User"
                    }

                    binding.tvEmail.text = it.email

                    AvatarUtils.loadUserAvatar(
                        context = requireContext(),
                        imageView = binding.ivUserAvatar,
                        photoUrl = it.photoUrl,
                        userId = it.uid,
                        isCircular = true
                    )
                } ?: run {
                    binding.tvDisplayName.text = "User"
                    binding.tvEmail.text = "No information"

                    AvatarUtils.loadUserAvatar(
                        context = requireContext(),
                        imageView = binding.ivUserAvatar,
                        photoUrl = null,
                        userId = "default_user",
                        isCircular = true
                    )
                }
            }
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authState.collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        binding.btnLogout.isEnabled = false
                        binding.btnLogout.text = "Signing out..."
                    }
                    is AuthResult.Error -> {
                        if (result.message == "Not logged in") {
                            navigateToAuthActivity()
                        } else {
                            binding.btnLogout.isEnabled = true
                            binding.btnLogout.text = "Sign Out"
                            Toast.makeText(
                                requireContext(),
                                "Sign out error: ${result.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    is AuthResult.Success -> {
                        binding.btnLogout.isEnabled = true
                        binding.btnLogout.text = "Sign Out"
                    }
                }
            }
        }
    }

    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirm Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        authViewModel.signOut()
    }

    private fun navigateToAuthActivity() {
        val intent = Intent(requireContext(), AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "UserProfileBottomSheet"
        
        fun newInstance(): UserProfileBottomSheetDialogFragment {
            return UserProfileBottomSheetDialogFragment()
        }
    }
}
