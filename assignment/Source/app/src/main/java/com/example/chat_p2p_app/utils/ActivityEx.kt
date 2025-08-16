package com.example.chat_p2p_app.utils

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.viewbinding.ViewBinding

fun AppCompatActivity.setPaddingEdgeToEdge(binding: ViewBinding) {
    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
                    or WindowInsetsCompat.Type.ime()
        )
        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = bars.top
            leftMargin = bars.left
            bottomMargin = bars.bottom
            rightMargin = bars.right
        }
        WindowInsetsCompat.CONSUMED
    }
}