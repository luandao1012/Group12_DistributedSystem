package com.example.chat_p2p_app.utils

import android.content.Context
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.chat_p2p_app.R
import kotlin.math.abs

object AvatarUtils {
    @DrawableRes
    private val ANIMAL_AVATARS = arrayOf(
        R.drawable.bear,
        R.drawable.cat,
        R.drawable.duck,
        R.drawable.panda,
        R.drawable.penguin,
        R.drawable.rabbit
    )

    @DrawableRes
    fun getRandomAnimalAvatar(userId: String): Int {
        val hash = abs(userId.hashCode())
        val index = hash % ANIMAL_AVATARS.size
        return ANIMAL_AVATARS[index]
    }

    fun loadUserAvatar(
        context: Context,
        imageView: ImageView,
        photoUrl: String?,
        userId: String,
        isCircular: Boolean = true
    ) {
        val fallbackAvatar = getRandomAnimalAvatar(userId)

        val requestOptions = RequestOptions()
            .placeholder(fallbackAvatar)
            .error(fallbackAvatar)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .apply {
                if (isCircular) {
                    circleCrop()
                }
            }

        if (photoUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(fallbackAvatar)
                .apply(requestOptions)
                .into(imageView)
        } else {
            Glide.with(context)
                .load(photoUrl)
                .apply(requestOptions)
                .into(imageView)
        }
    }

    fun loadUserAvatar(
        context: Context,
        imageView: ImageView,
        photoUrl: String?,
        userId: String
    ) {
        loadUserAvatar(context, imageView, photoUrl, userId, true)
    }

    fun getAnimalName(userId: String): String {
        val animalNames = arrayOf("Bear", "Cat", "Duck", "Panda", "Penguin", "Rabbit")
        val hash = abs(userId.hashCode())
        val index = hash % animalNames.size
        return animalNames[index]
    }
}
