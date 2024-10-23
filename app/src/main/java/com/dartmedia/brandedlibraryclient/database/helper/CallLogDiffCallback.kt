package com.dartmedia.brandedlibraryclient.database.helper

import androidx.recyclerview.widget.DiffUtil
import com.dartmedia.brandedlibraryclient.database.CallLog

class CallLogDiffCallback(
    private val oldLogList: List<CallLog>,
    private val newLogList: List<CallLog>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldLogList.size
    }

    override fun getNewListSize(): Int {
        return newLogList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldLogList[oldItemPosition].id == newLogList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldLogs = oldLogList[oldItemPosition]
        val newLogs = newLogList[newItemPosition]
        return oldLogs.id == newLogs.id && oldLogs.caller == newLogs.caller && oldLogs.duration == newLogs.duration
    }

}