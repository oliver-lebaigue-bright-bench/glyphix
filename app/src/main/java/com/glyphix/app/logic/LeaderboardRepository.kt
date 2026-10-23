package com.glyphix.app.logic

import android.util.Log
import com.glyphix.app.model.LeaderboardEntry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LeaderboardRepository {
    private val database = FirebaseDatabase.getInstance("https://bnmv-67120-default-rtdb.europe-west1.firebasedatabase.app").getReference("leaderboard")

    fun getTopUsers(limit: Int = 100): Flow<List<LeaderboardEntry>> = callbackFlow {
        val query = database.orderByChild("totalTimeMs").limitToLast(limit.coerceIn(1, 1000))
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val entries = snapshot.children.mapNotNull { child ->
                        try {
                            val direct = child.getValue(LeaderboardEntry::class.java)
                            if (direct != null && direct.totalTimeMs > 0) {
                                direct.copy(userId = child.key ?: direct.userId)
                            } else {
                                val uid = child.key ?: child.child("userId").getValue(String::class.java) ?: ""
                                val name = child.child("name").getValue(String::class.java) ?: "Anonymous"
                                val pic = child.child("profilePictureUrl").getValue(String::class.java)
                                val time = (child.child("totalTimeMs").value as? Number)?.toLong() ?: 0L
                                if (time > 0 && uid.isNotBlank()) {
                                    LeaderboardEntry(userId = uid, name = name, profilePictureUrl = pic, totalTimeMs = time)
                                } else direct
                            }
                        } catch (e: Exception) {
                            Log.e("LeaderboardRepo", "Error parsing entry ${child.key}", e)
                            null
                        }
                    }.filter { it.totalTimeMs > 0 }.sortedByDescending { it.totalTimeMs }

                    trySend(entries)
                } catch (e: Exception) {
                    Log.e("LeaderboardRepo", "Error processing leaderboard snapshot", e)
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LeaderboardRepo", "onCancelled: ${error.message}")
                close(error.toException())
            }
        }
        try {
            query.addValueEventListener(listener)
        } catch (e: Exception) {
            Log.e("LeaderboardRepo", "Failed to add leaderboard listener", e)
            close(e)
        }
        awaitClose { 
            try {
                query.removeEventListener(listener)
            } catch (e: Exception) {
                Log.e("LeaderboardRepo", "Error removing leaderboard listener", e)
            }
        }
        }.retryWhen { cause, attempt ->
        if (cause is CancellationException) false else {
            delay((1000L shl attempt.coerceAtMost(5).toInt()).coerceAtMost(30000L))
            true
        }
    }

    suspend fun updateScore(entry: LeaderboardEntry) {
        require(entry.userId == FirebaseAuth.getInstance().currentUser?.uid) {
            "Leaderboard updates require the authenticated user"
        }
        if (entry.totalTimeMs <= 0) return
        suspendCancellableCoroutine<Unit> { continuation ->
            database.child(entry.userId).runTransaction(object : Transaction.Handler {
                override fun doTransaction(data: MutableData): Transaction.Result {
                    val previous = (data.child("totalTimeMs").value as? Number)?.toLong() ?: 0L
                    // A delayed upload from another session must never reduce a score.
                    data.value = entry.copy(totalTimeMs = maxOf(previous, entry.totalTimeMs))
                    return Transaction.success(data)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (!continuation.isActive) return
                    if (error != null) continuation.resumeWithException(error.toException())
                    else if (!committed) continuation.resumeWithException(IllegalStateException("Score update aborted"))
                    else continuation.resume(Unit)
                }
            })
        }
    }
}
