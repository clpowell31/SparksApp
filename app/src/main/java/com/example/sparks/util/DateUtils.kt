package com.example.sparks.util

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun getChatHeaderDate(timestamp: Long): String {
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        // Same Day
        now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR) -> "Today"

        // Yesterday
        now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - msgTime.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"

        // Otherwise: "Jan 24"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Helper to check if two messages are on the same day
fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}