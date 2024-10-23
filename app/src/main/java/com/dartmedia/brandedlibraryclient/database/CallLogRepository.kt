package com.dartmedia.brandedlibraryclient.database

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.dartmedia.network.CallHistoryData
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CallLogRepository(application: Application) {
    private val mCallLogDao: CallLogDao
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()


    init {
        val db = CallLogDatabase.getDatabase(application)
        mCallLogDao = db.callLogDao()
    }

    fun getAllLogs(): LiveData<List<CallLog>> = mCallLogDao.getAllLogs()

    fun getLogByName(name: String): LiveData<List<CallLog>> {
        return mCallLogDao.getLogByName(name)
    }

    fun insertLog(newLog: CallLog) {
        try {
            executorService.execute { mCallLogDao.insert(newLog) }
            Log.d(TAG, "Call Log inserted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert log, $e")
        }

    }

    fun delete(log: CallLog) {
        try {
            executorService.execute { mCallLogDao.delete(log) }
            Log.d(TAG, "Call Log deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete log, $e")
        }
    }

    fun updateLog(log: CallLog) {
        try {
            executorService.execute { mCallLogDao.update(log) }
            Log.d(TAG, "Call Log updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update log, $e")
        }
    }

    fun syncCallLogs(logsFromServer: List<CallHistoryData>) {
        try {
            val callLogFromServer = logsFromServer.map {
                CallLog(
                    caller = it.name,
                    phone = it.phonenumber,
                    message = it.intent,
                    image = it.image_url,
                    date = it.date,
                    duration = it.duration.toString()
                )
            }
            executorService.execute { mCallLogDao.syncCallLogs(callLogFromServer) }
            Log.d(TAG, "Sync successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync, $e")
        }

    }

    companion object {
        var TAG = CallLogRepository::class.java.simpleName
    }
}