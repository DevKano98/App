package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val startTime: String, // HH:mm format, e.g. "09:00"
    val endTime: String,   // HH:mm format, e.g. "17:00"
    val repeatDays: Set<FocusDayOfWeek>,
    val enabled: Boolean = true,
    val appRuleIds: List<Long> = emptyList(),
    val websiteRuleIds: List<Long> = emptyList()
)
