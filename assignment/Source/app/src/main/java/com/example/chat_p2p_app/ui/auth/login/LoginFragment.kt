package com.example.chat_p2p_app.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.chat_p2p_app.R
import com.example.chat_p2p_app.common.ConstValue.TAG
import com.example.chat_p2p_app.databinding.FragmentLoginBinding
import com.example.chat_p2p_app.model.AuthResult
import com.example.chat_p2p_app.ui.auth.AuthViewModel
import com.example.chat_p2p_app.ui.auth.register.RegisterFragment
import com.example.chat_p2p_app.ui.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeAuthState()
    }

    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.signIn(email, password)
        }

        binding.tvRegister.setOnClickListener {
            activity?.supportFragmentManager?.commit {
                add(R.id.fragment_container_view, RegisterFragment())
                addToBackStack(null)
            }
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { result ->
                Log.i(TAG, "observeAuthState authState login: $result")
                when (result) {
                    is AuthResult.Loading -> {
                        binding.btnLogin.isEnabled = false
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    is AuthResult.Success -> {
                        binding.btnLogin.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                        activity?.startActivity(Intent(activity, HomeActivity::class.java))
                        activity?.finish()
                    }

                    is AuthResult.Error -> {
                        binding.btnLogin.isEnabled = true
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