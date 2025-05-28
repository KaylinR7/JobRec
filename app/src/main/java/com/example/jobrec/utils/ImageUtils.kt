package com.example.jobrec.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.jobrec.R

object ImageUtils {
    private const val TAG = "ImageUtils"
    
    /**
     * Load profile image from either Firebase Storage URL or base64 data
     * @param context Context for Glide
     * @param imageView Target ImageView
     * @param imageUrl Firebase Storage URL (nullable)
     * @param imageBase64 Base64 encoded image data (nullable)
     * @param isCircular Whether to apply circular crop
     * @param placeholderRes Placeholder resource ID
     */
    fun loadProfileImage(
        context: Context,
        imageView: ImageView,
        imageUrl: String?,
        imageBase64: String?,
        isCircular: Boolean = true,
        placeholderRes: Int = R.drawable.ic_person
    ) {
        when {
            !imageUrl.isNullOrEmpty() -> {
                // Load from Firebase Storage URL
                val glideRequest = Glide.with(context)
                    .load(imageUrl)
                    .placeholder(placeholderRes)
                    .error(placeholderRes)
                
                if (isCircular) {
                    glideRequest.transform(CircleCrop())
                }
                
                glideRequest.into(imageView)
                Log.d(TAG, "Loading image from URL: $imageUrl")
            }
            !imageBase64.isNullOrEmpty() -> {
                // Load from base64 stored in Firestore
                try {
                    val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    
                    if (bitmap != null) {
                        if (isCircular) {
                            // Use Glide to apply circular crop to bitmap
                            Glide.with(context)
                                .load(bitmap)
                                .transform(CircleCrop())
                                .placeholder(placeholderRes)
                                .error(placeholderRes)
                                .into(imageView)
                        } else {
                            imageView.setImageBitmap(bitmap)
                        }
                        Log.d(TAG, "Loading image from base64 data")
                    } else {
                        imageView.setImageResource(placeholderRes)
                        Log.w(TAG, "Failed to decode base64 image")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding base64 image", e)
                    imageView.setImageResource(placeholderRes)
                }
            }
            else -> {
                // No image available, use placeholder
                imageView.setImageResource(placeholderRes)
                Log.d(TAG, "No image data available, using placeholder")
            }
        }
    }
    
    /**
     * Load profile image from User object
     */
    fun loadProfileImage(
        context: Context,
        imageView: ImageView,
        user: com.example.jobrec.User,
        isCircular: Boolean = true,
        placeholderRes: Int = R.drawable.ic_person
    ) {
        loadProfileImage(
            context = context,
            imageView = imageView,
            imageUrl = user.profileImageUrl,
            imageBase64 = user.profileImageBase64,
            isCircular = isCircular,
            placeholderRes = placeholderRes
        )
    }
}
