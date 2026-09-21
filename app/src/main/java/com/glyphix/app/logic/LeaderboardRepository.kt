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
import kotlinx.coroutines.tasks.await

class LeaderboardRepository {
    private val database = FirebaseDatabase.getInstance("https://bnmv-67120-default-rtdb.europe-west1.firebasedatabase.app").getReference("leaderboard")

    fun getTopUsers(limit: Int = 100): Flow<List<LeaderboardEntry>> = callbackFlow {
        val query = database.orderByChild("totalTimeMs").limitToLast(limit)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val entries = snapshot.children.mapNotNull { child ->
                        try {
                            val direct = child.getValue(LeaderboardEntry::class.java)
                            if (direct != null && direct.totalTimeMs > 0) {
                                direct
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
                trySend(emptyList())
            }
        }
        try {
            query.addValueEventListener(listener)
        } catch (e: Exception) {
            Log.e("LeaderboardRepo", "Failed to add leaderboard listener", e)
            trySend(emptyList())
        }
        awaitClose { 
            try {
                query.removeEventListener(listener)
            } catch (e: Exception) {
                Log.e("LeaderboardRepo", "Error removing leaderboard listener", e)
            }
        }
    }

    suspend fun updateScore(entry: LeaderboardEntry) {
        if (entry.userId.isBlank()) return
        try {
            Log.d("LeaderboardRepo", "Updating score for user: ${entry.userId}")
            database.child(entry.userId).setValue(entry).await()
            Log.d("LeaderboardRepo", "Score updated successfully")
        } catch (e: Exception) {
            Log.e("LeaderboardRepo", "Failed to update score for user: ${entry.userId}", e)
        }
    }
}
