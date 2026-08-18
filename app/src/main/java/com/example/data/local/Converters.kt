package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.FocusDayOfWeek
import com.example.data.model.SessionStatus

class FocusLockTypeConverters {

    @TypeConverter
    fun fromDayOfWeekSet(days: Set<FocusDayOfWeek>?): String {
        return days?.joinToString(",") { it.name } ?: ""
    }

    @TypeConverter
    fun toDayOfWeekSet(data: String?): Set<FocusDayOfWeek> {
        if (data.isNullOrBlank()) return emptySet()
        return data.split(",")
            .mapNotNull { name ->
                runCatching { FocusDayOfWeek.valueOf(name.trim()) }.getOrNull()
            }
            .toSet()
    }

    @TypeConverter
    fun fromLongList(list: List<Long>?): String {
        return list?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toLongList(data: String?): List<Long> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
    }

    @TypeConverter
    fun fromSessionStatus(status: SessionStatus?): String {
        return status?.name ?: SessionStatus.ACTIVE.name
    }

    @TypeConverter
    fun toSessionStatus(data: String?): SessionStatus {
        return if (data.isNullOrBlank()) SessionStatus.ACTIVE
        else runCatching { SessionStatus.valueOf(data) }.getOrDefault(SessionStatus.ACTIVE)
    }
}
