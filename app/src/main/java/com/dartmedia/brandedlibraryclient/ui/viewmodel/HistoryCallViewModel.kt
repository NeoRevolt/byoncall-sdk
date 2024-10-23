package com.dartmedia.brandedlibraryclient.ui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartmedia.brandedlibraryclient.database.CallLog
import com.dartmedia.brandedlibraryclient.database.CallLogRepository
import com.dartmedia.network.CallHistoryData
import kotlinx.coroutines.launch

class HistoryCallViewModel(application: Application) : ViewModel() {
    private val mCallLogRepository: CallLogRepository = CallLogRepository(application)

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    fun getAllLogs(): LiveData<List<CallLog>> = mCallLogRepository.getAllLogs()

    fun getLogByName(name: String): LiveData<List<CallLog>> {
        return mCallLogRepository.getLogByName(name)
    }

    fun insert(newLog: CallLog) {
        mCallLogRepository.insertLog(newLog)
    }

    fun update(log: CallLog) {
        mCallLogRepository.updateLog(log)
    }

    fun delete(log: CallLog) {
        mCallLogRepository.delete(log)
    }

    fun syncCallLogs(serverLogsList: List<CallHistoryData>) {
        viewModelScope.launch {
            try {
                mCallLogRepository.syncCallLogs(serverLogsList)
            } catch (e: Exception) {
                _errorMessage.postValue(e.message)
            }
        }
    }

}