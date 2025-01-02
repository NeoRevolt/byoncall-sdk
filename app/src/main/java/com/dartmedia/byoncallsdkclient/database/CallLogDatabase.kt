package com.dartmedia.byoncallsdkclient.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [CallLog::class], version = 1, exportSchema = false)
abstract class CallLogDatabase : RoomDatabase() {

    abstract fun callLogDao(): CallLogDao


    companion object {

        @Volatile
        private var INSTANCE: CallLogDatabase? = null

        @JvmStatic
        fun getDatabase(context: Context): CallLogDatabase {
            if (INSTANCE == null) {
                synchronized(CallLogDatabase::class.java) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        CallLogDatabase::class.java,
                        "calllog_database"
                    )
                        .build()
                }
            }
            return INSTANCE as CallLogDatabase
        }
    }
}