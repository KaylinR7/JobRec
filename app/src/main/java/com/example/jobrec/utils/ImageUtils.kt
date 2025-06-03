package com.example.jobrec.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.jobrec.R
import java.io.ByteArrayOutputStream

object ImageUtils {
    private const val TAG = "ImageUtils"
    private const val MAX_IMAGE_SIZE = 1024 // Maximum width/height for compressed images
    private const val COMPRESSION_QUALITY = 85 // JPEG compression quality (0-100)

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
                // Load from Firebase Storage URL with optimizations
                val requestOptions = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(placeholderRes)
                    .error(placeholderRes)
                    .override(MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)

                val glideRequest = Glide.with(context)
                    .load(imageUrl)
                    .apply(requestOptions)

                if (isCircular) {
                    glideRequest.transform(CircleCrop())
                }

                glideRequest.into(imageView)
                Log.d(TAG, "Loading optimized image from URL: $imageUrl")
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

    /**
     * Load company logo with optimizations
     */
    fun loadCompanyLogo(
        context: Context,
        imageView: ImageView,
        logoUrl: String?,
        cornerRadius: Int = 8,
        placeholderRes: Int = R.drawable.ic_company_placeholder
    ) {
        val requestOptions = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(placeholderRes)
            .error(placeholderRes)
            .override(MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
            .transform(CenterCrop(), RoundedCorners(cornerRadius))

        Glide.with(context)
            .load(logoUrl)
            .apply(requestOptions)
            .into(imageView)

        Log.d(TAG, "Loading optimized company logo: $logoUrl")
    }

    /**
     * Load image with custom transformations and caching
     */
    fun loadOptimizedImage(
        context: Context,
        imageView: ImageView,
        imageUrl: String?,
        width: Int = MAX_IMAGE_SIZE,
        height: Int = MAX_IMAGE_SIZE,
        cornerRadius: Int = 0,
        isCircular: Boolean = false,
        placeholderRes: Int = R.drawable.ic_person
    ) {
        val requestOptions = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(placeholderRes)
            .error(placeholderRes)
            .override(width, height)

        val glideRequest = Glide.with(context)
            .load(imageUrl)
            .apply(requestOptions)

        when {
            isCircular -> glideRequest.transform(CircleCrop())
            cornerRadius > 0 -> glideRequest.transform(CenterCrop(), RoundedCorners(cornerRadius))
            else -> glideRequest.transform(CenterCrop())
        }

        glideRequest.into(imageView)
        Log.d(TAG, "Loading optimized image with custom settings: $imageUrl")
    }

    /**
     * Compress bitmap to reduce memory usage
     */
    fun compressBitmap(bitmap: Bitmap, quality: Int = COMPRESSION_QUALITY): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Resize bitmap to maximum dimensions while maintaining aspect ratio
     */
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int = MAX_IMAGE_SIZE, maxHeight: Int = MAX_IMAGE_SIZE): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val aspectRatio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxWidth
            newHeight = (maxWidth / aspectRatio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Convert bitmap to base64 with compression
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = COMPRESSION_QUALITY): String {
        val resizedBitmap = resizeBitmap(bitmap)
        val compressedBytes = compressBitmap(resizedBitmap, quality)
        return Base64.encodeToString(compressedBytes, Base64.DEFAULT)
    }

    /**
     * Preload images for better performance
     */
    fun preloadImages(context: Context, imageUrls: List<String>) {
        imageUrls.forEach { url ->
            if (url.isNotEmpty()) {
                Glide.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .preload(MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
            }
        }
        Log.d(TAG, "Preloaded ${imageUrls.size} images")
    }

    /**
     * Clear image cache to free up memory
     */
    fun clearImageCache(context: Context) {
        try {
            Glide.get(context).clearMemory()
            Thread {
                Glide.get(context).clearDiskCache()
            }.start()
            Log.d(TAG, "Cleared image cache")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing image cache", e)
        }
    }
}
