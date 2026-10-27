package com.nana.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nana.common.VoiceMacro
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(macro: VoiceMacro): Long

    @Update
    suspend fun update(macro: VoiceMacro): Int

    @Query("SELECT * FROM voice_macros WHERE triggerPhrase = :trigger")
    suspend fun getByTrigger(trigger: String): VoiceMacro?

    @Query("SELECT * FROM voice_macros WHERE enabled = 1")
    fun getEnabledMacros(): Flow<List<VoiceMacro>>

    @Query("SELECT * FROM voice_macros")
    fun getAllMacros(): Flow<List<VoiceMacro>>

    @Query("DELETE FROM voice_macros WHERE triggerPhrase = :trigger")
    suspend fun delete(trigger: String): Int

    @Query("UPDATE voice_macros SET lastUsedAt = :now, useCount = useCount + 1 WHERE triggerPhrase = :trigger")
    suspend fun markUsed(trigger: String, now: Long): Int
}
