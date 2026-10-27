package com.nana.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nana.common.ActionLog
import com.nana.common.VoiceMacro

@Database(
    entities = [ActionLog::class, VoiceMacro::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NanaDatabase : RoomDatabase() {
    abstract fun actionLogDao(): ActionLogDao
    abstract fun macroDao(): MacroDao

    companion object {
        @Volatile private var INSTANCE: NanaDatabase? = null

        fun getInstance(context: Context): NanaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NanaDatabase::class.java,
                    "nana.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
