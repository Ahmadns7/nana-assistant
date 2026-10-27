package com.nana.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nana.common.ActionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionLogDao {
    @Insert
    suspend fun insert(log: ActionLog): Long

    @Insert
    suspend fun insertAll(logs: List<ActionLog>)

    @Query("SELECT * FROM action_log WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getLogsForSession(sessionId: String): Flow<List<ActionLog>>

    @Query("SELECT * FROM action_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<ActionLog>>

    @Query("SELECT * FROM action_log WHERE timestamp > :since ORDER BY timestamp DESC")
    fun getLogsSince(since: Long): Flow<List<ActionLog>>

    @Query("DELETE FROM action_log WHERE timestamp < :before")
    suspend fun deleteOldLogs(before: Long): Int

    @Query("SELECT COUNT(*) FROM action_log")
    suspend fun getTotalCount(): Int
}
