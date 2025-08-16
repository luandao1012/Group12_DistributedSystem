package com.example.chat_p2p_app.ui.contacts

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.chat_p2p_app.ui.contacts.allusers.AllUsersFragment
import com.example.chat_p2p_app.ui.contacts.friends.FriendsFragment
import com.example.chat_p2p_app.ui.contacts.requests.FriendRequestsFragment

class ContactsViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FriendsFragment()
            1 -> FriendRequestsFragment()
            2 -> AllUsersFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
} 