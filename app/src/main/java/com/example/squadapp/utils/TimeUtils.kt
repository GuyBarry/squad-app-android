package com.example.squadapp.utils

import java.util.Date

object TimeUtils {
    fun getTimeAgoString(creationTime: Date): String {
        val currentTime = Date()
        val diffInMillis = currentTime.time - creationTime.time

        return when {
            diffInMillis < 60_000 -> "just now"
            diffInMillis < 3_600_000 -> {
                val minutes = (diffInMillis / 60_000).toInt()
                "$minutes ${if (minutes == 1) "minute" else "minutes"} ago"
            }
            diffInMillis < 86_400_000 -> {
                val hours = (diffInMillis / 3_600_000).toInt()
                "$hours ${if (hours == 1) "hour" else "hours"} ago"
            }
            diffInMillis < 2_592_000_000 -> {
                val days = (diffInMillis / 86_400_000).toInt()
                "$days ${if (days == 1) "day" else "days"} ago"
            }
            diffInMillis < 31_104_000_000 -> {
                val months = (diffInMillis / 2_592_000_000).toInt()
                "$months ${if (months == 1) "month" else "months"} ago"
            }
            else -> {
                val years = (diffInMillis / 31_104_000_000).toInt()
                "$years ${if (years == 1) "year" else "years"} ago"
            }
        }
    }
}