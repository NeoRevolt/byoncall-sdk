package com.dartmedia.brandedlibrary.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import com.dartmedia.brandedlibrary.database.CallLog
import com.dartmedia.brandedlibrary.database.CallLogRepository

class CallLogViewModel(application: Application) : ViewModel() {
    private val mCallLogRepository: CallLogRepository = CallLogRepository(application)

    fun insert(newLog: CallLog) {
        mCallLogRepository.insertLog(newLog)
    }

    fun update(log: CallLog) {
        mCallLogRepository.updateLog(log)
    }

    fun delete(log: CallLog) {
        mCallLogRepository.delete(log)
    }

    companion object {
        var TAG = CallLogViewModel::class.java.simpleName
    }
}