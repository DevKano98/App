package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lock_sessions")
data class LockSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val scheduleId: Long? = null,
    val name: String = "Focus Session",
    val startedAt: Long, // Epoch ms
    val endsAt: Long,    // Epoch ms
    val status: SessionStatus = SessionStatus.ACTIVE,
    val blockedAppCount: Int = 0,
    val blockedWebsiteCount: Int = 0
)
