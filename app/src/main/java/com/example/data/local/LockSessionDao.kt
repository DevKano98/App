package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LockSession
import com.example.data.model.SessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LockSessionDao {
    @Query("SELECT * FROM lock_sessions ORDER BY startedAt DESC")
    fun getAllLockSessions(): Flow<List<LockSession>>

    @Query("SELECT * FROM lock_sessions WHERE status = :status ORDER BY startedAt DESC")
    fun getSessionsByStatus(status: SessionStatus): Flow<List<LockSession>>

    @Query("SELECT * FROM lock_sessions WHERE status = 'ACTIVE' AND endsAt > :currentTime ORDER BY endsAt DESC LIMIT 1")
    fun getActiveSessionFlow(currentTime: Long): Flow<LockSession?>

    @Query("SELECT * FROM lock_sessions WHERE status = 'ACTIVE' ORDER BY endsAt DESC LIMIT 1")
    suspend fun getLatestActiveSession(): LockSession?

    @Query("SELECT * FROM lock_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): LockSession?

    @Query("SELECT * FROM lock_sessions WHERE startedAt >= :startOfDay AND startedAt <= :endOfDay")
    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<LockSession>>

    @Query("SELECT * FROM lock_sessions WHERE startedAt >= :startOfDay AND startedAt <= :endOfDay")
    suspend fun getSessionsForDaySync(startOfDay: Long, endOfDay: Long): List<LockSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: LockSession): Long

    @Update
    suspend fun updateSession(session: LockSession)

    @Query("UPDATE lock_sessions SET status = :newStatus WHERE id = :id")
    suspend fun updateSessionStatus(id: Long, newStatus: SessionStatus)

    @Query("UPDATE lock_sessions SET status = 'COMPLETED' WHERE status = 'ACTIVE' AND endsAt <= :currentTime")
    suspend fun completeExpiredSessions(currentTime: Long): Int

    @Delete
    suspend fun deleteSession(session: LockSession)

    @Query("DELETE FROM lock_sessions")
    suspend fun clearAllSessions()
}
