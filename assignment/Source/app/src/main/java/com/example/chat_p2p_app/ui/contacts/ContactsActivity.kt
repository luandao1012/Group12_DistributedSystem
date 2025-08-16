package com.example.chat_p2p_app.ui.contacts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.chat_p2p_app.databinding.ActivityContactsBinding
import com.example.chat_p2p_app.utils.setPaddingEdgeToEdge
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactsBinding
    private lateinit var viewPagerAdapter: ContactsViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setPaddingEdgeToEdge(binding)
        setupUI()
        setupViewPager()
        setupTabLayout()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupViewPager() {
        viewPagerAdapter = ContactsViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter
    }

    private fun setupTabLayout() {
        val tabTitles = arrayOf("Friends", "Requests", "All users")
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
} 