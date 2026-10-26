package com.glyphix.app.logic

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.glyphix.app.model.UserProfile
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class UserRepository {
    private val database = FirebaseDatabase.getInstance("https://bnmv-67120-default-rtdb.europe-west1.firebasedatabase.app").getReference("users")

    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val snapshot = database.child(userId).get().await()
            snapshot.getValue(UserProfile::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching profile", e)
            throw e
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        try {
            require(profile.userId == FirebaseAuth.getInstance().currentUser?.uid) {
                "Profile updates require the authenticated user"
            }
            suspendCancellableCoroutine<Unit> { continuation ->
                database.child(profile.userId).runTransaction(object : Transaction.Handler {
                    override fun doTransaction(data: MutableData): Transaction.Result {
                        val previousTime = (data.child("totalVisualizedTime").value as? Number)?.toLong() ?: 0L
                        val createdAt = (data.child("createdAt").value as? Number)?.toLong() ?: profile.createdAt
                        data.value = profile.copy(
                            totalVisualizedTime = maxOf(previousTime, profile.totalVisualizedTime),
                            createdAt = createdAt
                        )
                        return Transaction.success(data)
                    }
                    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                        if (!continuation.isActive) return
                        if (error != null) continuation.resumeWithException(error.toException())
                        else if (!committed) continuation.resumeWithException(IllegalStateException("Profile update aborted"))
                        else continuation.resume(Unit)
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving profile", e)
            throw e
        }
    }

    suspend fun uploadAvatarFromResource(userId: String, resourceId: Int, context: android.content.Context): String = withContext(Dispatchers.IO) {
        try {
            Log.d("UserRepository", "Encoding resource $resourceId to Base64")
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
                ?: throw Exception("Could not decode resource")
            encodeBitmapToBase64(bitmap)
        } catch (e: Exception) {
            Log.e("UserRepository", "Resource encoding failed", e)
            throw e
        }
    }

    suspend fun uploadProfilePicture(userId: String, imageUri: Uri, context: android.content.Context): String = withContext(Dispatchers.IO) {
        try {
            Log.d("UserRepository", "Encoding Uri $imageUri to Base64")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(imageUri).use { input ->
                requireNotNull(input) { "Could not open image" }
                BitmapFactory.decodeStream(input, null, bounds)
            }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image" }
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > 1024 || bounds.outHeight / sampleSize > 1024) sampleSize *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(imageUri).use { input ->
                requireNotNull(input) { "Could not open image" }
                BitmapFactory.decodeStream(input, null, options)
            } ?: throw Exception("Could not decode image")
            encodeBitmapToBase64(bitmap)
        } catch (e: Exception) {
            Log.e("UserRepository", "Uri encoding failed", e)
            throw e
        }
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        // 1. Scale down to 200x200 to keep RTDB lightweight
        val scaled = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
        
        // 2. Compress to high-quality JPEG
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        
        // 3. Convert to Base64 Data URL
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }
}
