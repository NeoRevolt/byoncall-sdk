package com.dartmedia.brandedlibrary.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface CallLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(callLog: CallLog)

    @Update
    fun update(callLog: CallLog)

    @Delete
    fun delete(callLog: CallLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllCallLogs(callLogsList: List<CallLog>)

    @Query("DELETE FROM calllog")
    fun deleteAllCallLogs()

    @Transaction
    fun syncCallLogs(callLogsList: List<CallLog>) {
        deleteAllCallLogs()
        insertAllCallLogs(callLogsList)
    }

    @Query("SELECT * FROM calllog ORDER BY id ASC")
    fun getAllLogs(): LiveData<List<CallLog>>

    @Query("SELECT * FROM calllog WHERE caller LIKE '%' || :name || '%' OR phone LIKE '%' || :name || '%' ORDER BY id DESC")
    fun getLogByName(name: String): LiveData<List<CallLog>>

}