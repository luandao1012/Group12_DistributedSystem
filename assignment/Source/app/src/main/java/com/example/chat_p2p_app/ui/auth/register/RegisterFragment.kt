package com.example.chat_p2p_app.ui.auth.register

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.chat_p2p_app.R
import com.example.chat_p2p_app.common.ConstValue.TAG
import com.example.chat_p2p_app.databinding.FragmentRegisterBinding
import com.example.chat_p2p_app.model.AuthResult
import com.example.chat_p2p_app.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeAuthState()
    }

    private fun setupUI() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val displayName = binding.etDisplayName.text.toString().trim()

            if (validateInputs(email, password, confirmPassword, displayName)) {
                viewModel.signUp(email, password, displayName)
            }
        }

        binding.tvLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun validateInputs(
        email: String,
        password: String,
        confirmPassword: String,
        displayName: String
    ): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return false
        }

        if (displayName.isEmpty()) {
            binding.etDisplayName.error = "Display name is required"
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            return false
        }

        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            return false
        }

        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = "Please confirm your password"
            return false
        }

        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return false
        }
        return true
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { result ->
                Log.i(TAG, "observeAuthState authState register: $result")
                when (result) {
                    is AuthResult.Loading -> {
                        binding.btnRegister.isEnabled = false
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    is AuthResult.Success -> {
                        binding.btnRegister.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "Registration successful! Please log in with your new account.", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    }

                    is AuthResult.Error -> {
                        binding.btnRegister.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        if (result.message != "Not logged in") {
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}