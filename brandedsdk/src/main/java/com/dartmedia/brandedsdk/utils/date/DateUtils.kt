package com.dartmedia.brandedlibrary.utils.date

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {
     fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    fun getCurrentDateDetailed(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

     fun getCurrentClock(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    fun extractTimeFromDate(dateString: String): String {
        val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US)
        val outputFormat = SimpleDateFormat("HH:mm", Locale.US)
        try {
            val date = inputFormat.parse(dateString)
            return outputFormat.format(date!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
}